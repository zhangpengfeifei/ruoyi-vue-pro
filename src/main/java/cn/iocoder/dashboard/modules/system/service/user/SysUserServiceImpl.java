package cn.iocoder.dashboard.modules.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.dashboard.common.enums.CommonStatusEnum;
import cn.iocoder.dashboard.common.exception.ServiceException;
import cn.iocoder.dashboard.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.dashboard.common.pojo.PageResult;
import cn.iocoder.dashboard.modules.infra.service.file.InfFileService;
import cn.iocoder.dashboard.modules.system.controller.user.vo.user.SysUserCreateReqVO;
import cn.iocoder.dashboard.modules.system.controller.user.vo.user.SysUserExportReqVO;
import cn.iocoder.dashboard.modules.system.controller.user.vo.user.SysUserImportExcelVO;
import cn.iocoder.dashboard.modules.system.controller.user.vo.user.SysUserImportRespVO;
import cn.iocoder.dashboard.modules.system.controller.user.vo.user.SysUserPageReqVO;
import cn.iocoder.dashboard.modules.system.controller.user.vo.user.SysUserProfileUpdateReqVO;
import cn.iocoder.dashboard.modules.system.controller.user.vo.user.SysUserUpdateReqVO;
import cn.iocoder.dashboard.modules.system.convert.user.SysUserConvert;
import cn.iocoder.dashboard.modules.system.dal.dataobject.dept.SysDeptDO;
import cn.iocoder.dashboard.modules.system.dal.dataobject.dept.SysPostDO;
import cn.iocoder.dashboard.modules.system.dal.dataobject.user.SysUserDO;
import cn.iocoder.dashboard.modules.system.dal.mysql.user.SysUserMapper;
import cn.iocoder.dashboard.modules.system.service.dept.SysDeptService;
import cn.iocoder.dashboard.modules.system.service.dept.SysPostService;
import cn.iocoder.dashboard.modules.system.service.permission.SysPermissionService;
import cn.iocoder.dashboard.util.collection.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.dashboard.modules.system.enums.SysErrorCodeConstants.*;


/**
 * 用户 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class SysUserServiceImpl implements SysUserService {

    @Resource
    private SysUserMapper userMapper;

    @Resource
    private SysDeptService deptService;
    @Resource
    private SysPostService postService;
    @Resource
    private SysPermissionService permissionService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private InfFileService fileService;

    @Override
    public Long createUser(SysUserCreateReqVO reqVO) {
        // 校验正确性
        this.checkCreateOrUpdate(null, reqVO.getUsername(), reqVO.getMobile(), reqVO.getEmail(),
                reqVO.getDeptId(), reqVO.getPostIds());
        // 插入用户
        SysUserDO user = SysUserConvert.INSTANCE.convert(reqVO);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus()); // 默认开启
        user.setPassword(passwordEncoder.encode(reqVO.getPassword())); // 加密密码
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public void updateUser(SysUserUpdateReqVO reqVO) {
        // 校验正确性
        this.checkCreateOrUpdate(reqVO.getId(), reqVO.getUsername(), reqVO.getMobile(), reqVO.getEmail(),
                reqVO.getDeptId(), reqVO.getPostIds());
        // 更新用户
        SysUserDO updateObj = SysUserConvert.INSTANCE.convert(reqVO);
        userMapper.updateById(updateObj);
    }

    @Override
    public void updateUserProfile(SysUserProfileUpdateReqVO reqVO) {
        // 校验正确性
        this.checkUserExists(reqVO.getId());
        this.checkEmailUnique(reqVO.getId(), reqVO.getEmail());
        this.checkMobileUnique(reqVO.getId(), reqVO.getMobile());
        // 校验填写密码
        String encode = null;
        if (this.checkOldPassword(reqVO.getId(), reqVO.getOldPassword(), reqVO.getNewPassword())) {
            // 更新密码
            encode = passwordEncoder.encode(reqVO.getNewPassword());
        }
        SysUserDO updateObj = SysUserConvert.INSTANCE.convert(reqVO);
        if (StrUtil.isNotBlank(encode)) {
            updateObj.setPassword(encode);
        }
        userMapper.updateById(updateObj);
    }

    @Override
    public void updateUserAvatar(Long id, InputStream avatarFile) {
        this.checkUserExists(id);
        // 存储文件
        String avatar = fileService.createFile(IdUtil.fastUUID(), IoUtil.readBytes(avatarFile));
        // 更新路径
        SysUserDO sysUserDO = new SysUserDO();
        sysUserDO.setId(id);
        sysUserDO.setAvatar(avatar);
        userMapper.updateById(sysUserDO);
    }

    @Override
    public void updateUserPassword(Long id, String password) {
        // 校验用户存在
        this.checkUserExists(id);
        // 更新密码
        SysUserDO updateObj = new SysUserDO();
        updateObj.setId(id);
        updateObj.setPassword(passwordEncoder.encode(password)); // 加密密码
        userMapper.updateById(updateObj);
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        // 校验用户存在
        this.checkUserExists(id);
        // 更新状态
        SysUserDO updateObj = new SysUserDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        userMapper.updateById(updateObj);
        // 删除用户关联数据
        permissionService.processUserDeleted(id);
    }

    @Override
    public void deleteUser(Long id) {
        // 校验用户存在
        this.checkUserExists(id);
        // 删除用户
        userMapper.deleteById(id);
    }

    @Override
    public SysUserDO getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public SysUserDO getUser(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public PageResult<SysUserDO> getUserPage(SysUserPageReqVO reqVO) {
        return userMapper.selectPage(reqVO, this.getDeptCondition(reqVO.getDeptId()));
    }

    @Override
    public List<SysUserDO> getUsers(SysUserExportReqVO reqVO) {
        return userMapper.selectList(reqVO, this.getDeptCondition(reqVO.getDeptId()));
    }

    @Override
    public List<SysUserDO> getUsers(Collection<Long> ids) {
        return userMapper.selectBatchIds(ids);
    }

    @Override
    public List<SysUserDO> getUsersByNickname(String nickname) {
        return userMapper.selectListByNickname(nickname);
    }

    @Override
    public List<SysUserDO> getUsersByUsername(String username) {
        return userMapper.selectListByUsername(username);
    }

    /**
     * 获得部门条件：查询指定部门的子部门编号们，包括自身
     *
     * @param deptId 部门编号
     * @return 部门编号集合
     */
    private Set<Long> getDeptCondition(Long deptId) {
        if (deptId == null) {
            return Collections.emptySet();
        }
        Set<Long> deptIds = CollectionUtils.convertSet(deptService.getDeptsByParentIdFromCache(
            deptId, true), SysDeptDO::getId);
        deptIds.add(deptId); // 包括自身
        return deptIds;
    }

    private void checkCreateOrUpdate(Long id, String username, String mobile, String email,
                                     Long deptId, Set<Long> postIds) {
        // 校验用户存在
        this.checkUserExists(id);
        // 校验用户名唯一
        this.checkUsernameUnique(id, username);
        // 校验手机号唯一
        this.checkMobileUnique(id, mobile);
        // 校验邮箱唯一
        this.checkEmailUnique(id, email);
        // 校验部门处于开启状态
        this.checkDeptEnable(deptId);
        // 校验岗位处于开启状态
        this.checkPostEnable(postIds);
    }

    private void checkUserExists(Long id) {
        if (id == null) {
            return;
        }
        SysUserDO user = userMapper.selectById(id);
        if (user == null) {
            throw ServiceExceptionUtil.exception(USER_NOT_EXISTS);
        }
    }

    private void checkUsernameUnique(Long id, String username) {
        if (StrUtil.isNotBlank(username)) {
            return;
        }
        SysUserDO user = userMapper.selectByUsername(username);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw ServiceExceptionUtil.exception(USER_USERNAME_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(USER_USERNAME_EXISTS);
        }
    }

    private void checkEmailUnique(Long id, String email) {
        if (StrUtil.isNotBlank(email)) {
            return;
        }
        SysUserDO user = userMapper.selectByEmail(email);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw ServiceExceptionUtil.exception(USER_EMAIL_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(USER_EMAIL_EXISTS);
        }
    }

    private void checkMobileUnique(Long id, String email) {
        if (StrUtil.isNotBlank(email)) {
            return;
        }
        SysUserDO user = userMapper.selectByMobile(email);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw ServiceExceptionUtil.exception(USER_MOBILE_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw ServiceExceptionUtil.exception(USER_MOBILE_EXISTS);
        }
    }

    private void checkDeptEnable(Long deptId) {
        if (deptId == null) { // 允许不选择
            return;
        }
        SysDeptDO dept = deptService.getDept(deptId);
        if (dept == null) {
            throw ServiceExceptionUtil.exception(DEPT_NOT_FOUND);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(dept.getStatus())) {
            throw ServiceExceptionUtil.exception(DEPT_NOT_ENABLE);
        }
    }

    private void checkPostEnable(Set<Long> postIds) {
        if (CollUtil.isEmpty(postIds)) { // 允许不选择
            return;
        }
        List<SysPostDO> posts = postService.getPosts(postIds, null);
        if (CollUtil.isEmpty(posts)) {
            throw ServiceExceptionUtil.exception(POST_NOT_FOUND);
        }
        Map<Long, SysPostDO> postMap = CollectionUtils.convertMap(posts, SysPostDO::getId);
        postIds.forEach(postId -> {
            SysPostDO post = postMap.get(postId);
            if (post == null) {
                throw ServiceExceptionUtil.exception(POST_NOT_FOUND);
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(post.getStatus())) {
                throw ServiceExceptionUtil.exception(POST_NOT_ENABLE, post.getName());
            }
        });
    }

    /**
     * 校验旧密码、新密码
     *
     * @param id          用户 id
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 校验结果
     */
    private boolean checkOldPassword(Long id, String oldPassword, String newPassword) {
        if (id == null || StrUtil.isBlank(oldPassword) || StrUtil.isBlank(newPassword)) {
            return false;
        }
        SysUserDO user = userMapper.selectById(id);
        if (user == null) {
            throw ServiceExceptionUtil.exception(USER_NOT_EXISTS);
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw ServiceExceptionUtil.exception(USER_PASSWORD_FAILED);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 添加事务，异常则回滚所有导入
    public SysUserImportRespVO importUsers(List<SysUserImportExcelVO> importUsers, boolean isUpdateSupport) {
        if (CollUtil.isEmpty(importUsers)) {
            throw ServiceExceptionUtil.exception(USER_IMPORT_LIST_IS_EMPTY);
        }
        SysUserImportRespVO respVO = SysUserImportRespVO.builder().createUsernames(new ArrayList<>())
            .updateUsernames(new ArrayList<>()).failureUsernames(new LinkedHashMap<>()).build();
        importUsers.forEach(importUser -> {
            // 校验，判断是否有不符合的原因
            try {
                checkCreateOrUpdate(null, null, importUser.getMobile(), importUser.getEmail(),
                    importUser.getDeptId(), null);
            } catch (ServiceException ex) {
                respVO.getFailureUsernames().put(importUser.getUsername(), ex.getMessage());
                return;
            }
            // 判断如果不存在，在进行插入
            SysUserDO existUser = userMapper.selectByUsername(importUser.getUsername());
            if (existUser == null) {
                // TODO 芋艿：初始密码
                userMapper.insert(SysUserConvert.INSTANCE.convert(importUser));
                respVO.getCreateUsernames().add(importUser.getUsername());
                return;
            }
            // 如果存在，判断是否允许更新
            if (!isUpdateSupport) {
                respVO.getFailureUsernames().put(importUser.getUsername(), USER_USERNAME_EXISTS.getMessage());
                return;
            }
            SysUserDO updateUser = SysUserConvert.INSTANCE.convert(importUser);
            updateUser.setId(existUser.getId());
            userMapper.updateById(updateUser);
            respVO.getUpdateUsernames().add(importUser.getUsername());
        });
        return respVO;
    }

}
