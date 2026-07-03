package org.example.shop1.controller;

import org.example.shop1.model.dto.CategoryRequestDto;
import org.example.shop1.model.dto.CategoryResponseDto;
import org.example.shop1.model.entity.Category;
import org.example.shop1.model.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }
    // متد getTree را تغییر دهید تا بر اساس نوع فیلتر کند
    @GetMapping("/tree")
    public List<CategoryResponseDto> getTree(@RequestParam(required = false) String type) {
        return service.getTree(type); // سرویس باید آپدیت شود
    }
    @PostMapping
    public Category create(@RequestBody CategoryRequestDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable String id, @RequestBody CategoryRequestDto dto) {

        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

//    @GetMapping("/tree")
//    public List<CategoryResponseDto> getTree() {
//        return service.getTree();
//    }

    @PutMapping("/{id}/move")     public Category move(@PathVariable String id, @RequestBody CategoryRequestDto dto) {
        return service.moveCategory(id, dto.getNewParentId());
    }
}
