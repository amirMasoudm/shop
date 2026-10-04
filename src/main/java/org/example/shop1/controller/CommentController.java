package org.example.shop1.controller;

import org.example.shop1.config.SecurityUtils;
import org.example.shop1.model.dto.CommentRequest;
import org.example.shop1.model.entity.Comment;
import org.example.shop1.model.entity.User;
import org.example.shop1.model.service.CommentService;
import org.example.shop1.model.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin
public class CommentController {

    private final CommentService commentService;
    private final UserService userService; // اضافه شدن سرویس کاربر

    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    // ۱. ثبت نظر توسط کاربر
    @PostMapping
    public ResponseEntity<?> addComment(@RequestBody CommentRequest req) {
        try {
            User currentUser = userService.getCurrentAuthenticatedUser();

            // پاکسازی متن نظر از تگ‌های مخرب HTML
            req.setText(SecurityUtils.clean(req.getText()));

            String userId = currentUser.getId();
            String userName = currentUser.getFirstName() + " " + currentUser.getLastName();

            Comment created = commentService.submitComment(req, userId, userName);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("برای ثبت نظر ابتدا وارد حساب کاربری خود شوید.");
        }
    }
    // ۲. دریافت نظرات تایید شده یک محصول (دسترسی عمومی)
    @GetMapping("/product/{pid}")
    public ResponseEntity<List<Comment>> getProductComments(@PathVariable String pid) {
        List<Comment> comments = commentService.getApprovedCommentsByProduct(pid);
        return ResponseEntity.ok(comments);
    }

    // ۳. دریافت نظرات بر اساس وضعیت برای پنل ادمین (مثال: PENDING)
    @GetMapping("/admin/list")
    public ResponseEntity<List<Comment>> getCommentsForAdmin(@RequestParam(defaultValue = "PENDING") String status) {
        List<Comment> comments = commentService.getCommentsByStatusForAdmin(status);
        return ResponseEntity.ok(comments);
    }

    // ۴. تایید نظر توسط ادمین
    @PutMapping("/admin/{id}/approve")
    public ResponseEntity<Void> approveComment(@PathVariable String id) {
        commentService.approveComment(id);
        return ResponseEntity.ok().build();
    }

    // ۵. رد نظر توسط ادمین
    @PutMapping("/admin/{id}/reject")
    public ResponseEntity<Void> rejectComment(@PathVariable String id) {
        commentService.rejectComment(id);
        return ResponseEntity.ok().build();
    }

    // ۶. پاسخ ادمین به یک نظر
    @PutMapping("/admin/{id}/reply")
    public ResponseEntity<Void> replyToComment(@PathVariable String id, @RequestBody String replyText) {
        commentService.replyToComment(id, replyText);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/my")
    public ResponseEntity<?> getMyComments() {
        try {
            // گرفتن کاربر لاگین شده از سیستم
            User currentUser = userService.getCurrentAuthenticatedUser();
            List<Comment> comments = commentService.getMyComments(currentUser.getId());
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("کاربر احراز هویت نشده است.");
        }
    }
    // دریافت تمام نظرات یک کاربر خاص برای پرونده مشتری در پنل ادمین
    @GetMapping("/admin/user/{userId}")
    public ResponseEntity<List<Comment>> getUserCommentsForAdmin(@PathVariable String userId) {
        try {
            List<Comment> comments = commentService.getCommentsByUserIdForAdmin(userId);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}