package cn.iocoder.dashboard.modules.system.service.notice;

import cn.iocoder.dashboard.common.pojo.PageResult;
import cn.iocoder.dashboard.modules.system.controller.notice.vo.SysNoticeCreateReqVO;
import cn.iocoder.dashboard.modules.system.controller.notice.vo.SysNoticePageReqVO;
import cn.iocoder.dashboard.modules.system.controller.notice.vo.SysNoticeUpdateReqVO;
import cn.iocoder.dashboard.modules.system.dal.dataobject.notice.SysNoticeDO;

/**
 * 通知公告 Service 接口
 */
public interface SysNoticeService {

    /**
     * 创建岗位公告公告
     *
     * @param reqVO 岗位公告公告信息
     * @return 岗位公告公告编号
     */
    Long createNotice(SysNoticeCreateReqVO reqVO);

    /**
     * 更新岗位公告公告
     *
     * @param reqVO 岗位公告公告信息
     */
    void updateNotice(SysNoticeUpdateReqVO reqVO);

    /**
     * 删除岗位公告公告信息
     *
     * @param id 岗位公告公告编号
     */
    void deleteNotice(Long id);

    /**
     * 获得岗位公告公告分页列表
     *
     * @param reqVO 分页条件
     * @return 部门分页列表
     */
    PageResult<SysNoticeDO> pageNotices(SysNoticePageReqVO reqVO);

    /**
     * 获得岗位公告公告信息
     *
     * @param id 岗位公告公告编号
     * @return 岗位公告公告信息
     */
    SysNoticeDO getNotice(Long id);

}
