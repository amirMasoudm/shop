package org.example.shop1.model.service;

import org.example.shop1.model.dto.CommentRequest;
import org.example.shop1.model.entity.Comment;
import org.example.shop1.model.entity.Order;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.reposritory.CommentRepository;
import org.example.shop1.model.reposritory.OrderRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository; // تزریق فاکتورها

    public CommentService(CommentRepository commentRepository,
                          ProductRepository productRepository,
                          OrderRepository orderRepository) {
        this.commentRepository = commentRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public Comment submitComment(CommentRequest req, String userId, String fullName) {
        Comment c = new Comment();
        c.setProductId(req.getProductId());
        c.setUserId(userId);
        c.setUserName(fullName); // ذخیره نام واقعی (مثلاً علی محمدی)
        c.setText(req.getText());
        c.setRating(req.getRating());
        c.setStatus("PENDING");

        // --- منطق هوشمند تشخیص خریدار و اتصال فاکتور ---
        List<Order> userOrders = orderRepository.findByUserId(userId);

        boolean hasBought = false;
        String linkedOrderId = null;

        if (userOrders != null) {
            for (Order order : userOrders) {
                // فقط سفارشاتی که پرداخت شده‌اند (یا مراحل بعد) را چک می‌کنیم
                if (!"PENDING_PAYMENT".equals(order.getStatus().name())) {
                    boolean foundProduct = order.getItems().stream()
                            .anyMatch(item -> item.getProductId().equals(req.getProductId()));

                    if (foundProduct) {
                        hasBought = true;
                        linkedOrderId = order.getId();
                        break; // اولین فاکتور خرید پیدا شد
                    }
                }
            }
        }

        c.setIsBuyer(hasBought);
        c.setOrderId(linkedOrderId); // این فیلد را در کلاس Comment اضافه کنید (در مرحله بعد)

        return commentRepository.save(c);
    }
    public List<Comment> getCommentsByUserIdForAdmin(String userId) {
        return commentRepository.findByUserId(userId);
    }
    // متد دریافت نظرات تایید شده یک محصول جهت نمایش در فرانت‌اند
    public List<Comment> getApprovedCommentsByProduct(String productId) {
        return commentRepository.findByProductIdAndStatus(productId, "APPROVED");
    }

    // متد دریافت تمام نظرات بر اساس وضعیت برای پنل ادمین
    public List<Comment> getCommentsByStatusForAdmin(String status) {
        return commentRepository.findByStatus(status);
    }

    @Transactional
    public void approveComment(String commentId) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("کامنت پیدا نشد"));
        c.setStatus("APPROVED");
        commentRepository.save(c);

        // بروزرسانی میانگین امتیاز محصول پس از تایید کامنت جدید
        updateProductRating(c.getProductId());
    }

    @Transactional
    public void rejectComment(String commentId) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("کامنت پیدا نشد"));
        c.setStatus("REJECTED");
        commentRepository.save(c);
    }

    @Transactional
    public void replyToComment(String commentId, String replyText) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("کامنت پیدا نشد"));
        c.setAdminReply(replyText);
        commentRepository.save(c);
    }

    private void updateProductRating(String productId) {
        List<Comment> approvedComments = commentRepository.findByProductIdAndStatus(productId, "APPROVED");
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("محصول پیدا نشد"));

        double avg = approvedComments.stream()
                .mapToInt(Comment::getRating)
                .average().orElse(0.0);

        p.setAverageRating(avg);
        p.setReviewCount((long) approvedComments.size());
        productRepository.save(p);
    }
    public List<Comment> getMyComments(String userId) {
        return commentRepository.findByUserId(userId);
    }
}