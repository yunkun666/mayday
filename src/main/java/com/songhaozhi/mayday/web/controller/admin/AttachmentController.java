package com.songhaozhi.mayday.web.controller.admin;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.github.pagehelper.PageInfo;
import com.songhaozhi.mayday.model.domain.Attachment;
import com.songhaozhi.mayday.model.domain.Log;
import com.songhaozhi.mayday.model.dto.JsonResult;
import com.songhaozhi.mayday.model.dto.LogConstant;
import com.songhaozhi.mayday.model.enums.MaydayEnums;
import com.songhaozhi.mayday.service.AttachmentService;

import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.servlet.ServletUtil;
import net.coobird.thumbnailator.Thumbnails;

/**
 * @author 作者:宋浩志
 * @createDate 创建时间：2018年9月7日 上午10:35:55 附件
 */
@RequestMapping(value = "/admin/attachment")
@Controller
public class AttachmentController extends BaseController {
	@Autowired
	private AttachmentService attachmentService;
	/**
	 * 跳转附件页面并显示所有图片
	 * @return
	 */
	@GetMapping
	public String attachment(Model model,@RequestParam(value="page",defaultValue="1") int page,@RequestParam(value="limit", defaultValue="18") int limit) {
		PageInfo<Attachment> info= attachmentService.getAttachment(page, limit);
		model.addAttribute("info", info);
		return "/admin/admin_attachment";
	}
	
	/**
	 * 上传附件
	 * @param file
	 * @param request
	 * @return
	 */
	@RequestMapping("/upload")
	@ResponseBody
	public JsonResult upload(@RequestParam(value = "file") MultipartFile file, HttpServletRequest request) {
		return uploadAttachment(file, request);
	}
	
	
	/**
	 * 上传功能
	 * @param file
	 * @param request
	 * @return
	 */
	public JsonResult uploadAttachment(@RequestParam(value = "file") MultipartFile file, HttpServletRequest request) {
		if (!file.isEmpty()) {
			try {
				// 获取项目真实路径src/main/resources
				File path = new File(ResourceUtils.getURL("classpath:").getPath());
				// 上传路径
				StringBuffer sb = new StringBuffer("upload/");
				// 获取时间，以年月创建目录
				Date date = DateUtil.date();
				sb.append(DateUtil.thisYear()).append("/").append(DateUtil.thisMonth()+1).append("/");
				File mediaPath = new File(path.getAbsolutePath(), sb.toString());
				// 如果没有该目录则创建
				if (!mediaPath.exists()) {
					mediaPath.mkdirs();
				}
				System.out.println("路径++++++"+mediaPath);
				SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
				// 生成文件名称
				String nameSuffix = file.getOriginalFilename().substring(0, file.getOriginalFilename().lastIndexOf("."))
						.replaceAll(" ", "_").replaceAll(",", "") + format.format(DateUtil.date())
						+ new Random().nextInt(1000);
				// 文件后缀
				String fileSuffix = file.getOriginalFilename()
						.substring(file.getOriginalFilename().lastIndexOf(".") + 1);
				// 上传文件名
				String fileName = nameSuffix + "." + fileSuffix;
				file.transferTo(new File(mediaPath.toString(), fileName));
				// 压缩图片
				Thumbnails.of(new StringBuffer(mediaPath.getAbsolutePath()).append("/").append(fileName).toString())
						.size(256, 256).keepAspectRatio(false).toFile(new StringBuffer(mediaPath.getAbsolutePath()).append("/").append(nameSuffix)
								.append("_small.").append(fileSuffix).toString());
				// 添加数据库
				Attachment attachment = new Attachment();
				attachment.setPictureName(fileName);
				attachment.setPicturePath(new StringBuffer("/upload/").append(DateUtil.thisYear()).append("/").append(DateUtil.thisMonth()+1).append("/").append(fileName).toString());
				attachment.setPictureType(file.getContentType());
				attachment.setPictureCreateDate(date);
				attachment.setPictureSuffix(new StringBuffer().append(".").append(fileSuffix).toString());
				attachment.setPictureSmallPath(new StringBuffer("/upload/").append(DateUtil.thisYear()).append("/").append(DateUtil.thisMonth()+1).append("/").append(nameSuffix).append("_small.")
						.append(fileSuffix).toString());
				attachmentService.save(attachment);
				//添加日志
				logService.save(new Log(LogConstant.UPLOAD_ATTACHMENT, LogConstant.UPLOAD_SUCCESS,
						ServletUtil.getClientIP(request), DateUtil.date()));
			} catch (Exception e) {
				e.printStackTrace();
				log.error("上传附件错误" + e.getMessage());
				return new JsonResult(false, MaydayEnums.ERROR.getCode(), "系统未知错误");
			}
		} else {
			return new JsonResult(false, MaydayEnums.OPERATION_ERROR.getCode(), "文件不能为空");
		}
		return new JsonResult(true, MaydayEnums.OPERATION_SUCCESS.getCode(), "上传成功");
	}

}
