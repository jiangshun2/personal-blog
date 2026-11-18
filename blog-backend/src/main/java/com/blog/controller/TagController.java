package com.blog.controller;

import com.blog.common.Result;
import com.blog.service.TagService;
import com.blog.vo.TagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签前台接口（无需登录）
 */
@Tag(name = "标签（前台）", description = "标签列表")
@RestController
@RequestMapping("/api/tag")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /** 标签列表（带多级缓存，含每个标签下已发布文章数量） */
    @Operation(summary = "标签列表", description = "带多级缓存，含文章数量")
    @GetMapping("/list")
    public Result<List<TagVO>> list() {
        return Result.ok(tagService.listTags());
    }
}
