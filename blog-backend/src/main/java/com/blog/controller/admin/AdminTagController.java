package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.dto.TagSaveRequest;
import com.blog.service.TagService;
import com.blog.vo.TagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签后台管理接口（需登录）
 */
@Tag(name = "标签管理（后台）", description = "标签增删改查")
@RestController
@RequestMapping("/api/admin/tag")
@RequiredArgsConstructor
public class AdminTagController {

    private final TagService tagService;

    /** 标签列表（含文章数量） */
    @Operation(summary = "后台标签列表")
    @GetMapping("/list")
    public Result<List<TagVO>> list() {
        return Result.ok(tagService.listTags());
    }

    /** 新增标签 */
    @Operation(summary = "新增标签")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody TagSaveRequest request) {
        tagService.createTag(request);
        return Result.ok();
    }

    /** 修改标签 */
    @Operation(summary = "修改标签")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "标签 ID") @PathVariable Long id,
                               @Valid @RequestBody TagSaveRequest request) {
        tagService.updateTag(id, request);
        return Result.ok();
    }

    /** 删除标签：同时清理文章-标签关联 */
    @Operation(summary = "删除标签", description = "同时清理文章-标签关联")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "标签 ID") @PathVariable Long id) {
        tagService.deleteTag(id);
        return Result.ok();
    }
}
