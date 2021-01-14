package cn.iocoder.dashboard.modules.system.controller.common;

import cn.hutool.core.io.IoUtil;
import cn.iocoder.dashboard.common.pojo.CommonResult;
import cn.iocoder.dashboard.modules.system.dal.mysql.dataobject.common.SysFileDO;
import cn.iocoder.dashboard.modules.system.service.common.SysFileService;
import cn.iocoder.dashboard.util.servlet.ServletUtils;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static cn.iocoder.dashboard.common.pojo.CommonResult.success;

@Api(tags = "文件 API")
@RestController
@RequestMapping("/system/file")
@Slf4j
public class SysFileController {

    @Resource
    private SysFileService fileService;

    @PostMapping("/upload")
    public CommonResult<String> uploadFile(@RequestParam("file") MultipartFile file,
                                           @RequestParam("path") String path) throws IOException {
        return success(fileService.createFile(path, IoUtil.readBytes(file.getInputStream())));
    }

    @GetMapping("/get/{path}")
    public void getFile(HttpServletResponse response, @PathVariable("path") String path) throws IOException {
        SysFileDO file = fileService.getFile(path);
        if (file == null) {
            log.warn("[getFile][path({}) 文件不存在]", path);
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        ServletUtils.writeAttachment(response, path, file.getContent());
    }

}
