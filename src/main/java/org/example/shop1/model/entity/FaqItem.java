package org.example.shop1.model.entity;

/**
 * پرسش متداول محصول (تعبیه‌شده در Product).
 * در صفحه‌ی محصول رندر می‌شود و اسکیمای FAQPage از رویش ساخته می‌شود.
 */
public class FaqItem {

    private String question;
    private String answer;

    public FaqItem() {}

    public FaqItem(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
}
