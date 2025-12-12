package org.example.shop1.controller;

import org.example.shop1.model.dto.AiRequestDto;
import org.example.shop1.model.dto.AiResponseDto;
import org.example.shop1.model.service.OpenAIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin
public class AIController {

    private final OpenAIService openAIService;

    public AIController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @PostMapping("/generate-description")
    public ResponseEntity<AiResponseDto> generateDescription(@RequestBody AiRequestDto reqDto) {
        String prompt = buildPrompt(reqDto);
        String desc = openAIService.generateDescription(prompt);
        return ResponseEntity.ok(new AiResponseDto(desc));
    }

    private String buildPrompt(AiRequestDto r) {
        StringBuilder sb = new StringBuilder();

        sb.append("لطفاً یک متن توضیحات محصول حرفه‌ای، جذاب و مناسب فروشگاه آنلاین تولید کن (فارسی). ");
        sb.append("متن باید شامل مزایا، ویژگی‌ها و کاربرد محصول باشد و bullet points برای نقاط قوت ارائه شود.\n\n");

        // در صورت وجود توضیح کاربر، اولویت با آن است
        if (r.getUserDescription() != null && !r.getUserDescription().isEmpty()) {
            sb.append("توضیح کاربر: ").append(r.getUserDescription()).append("\n");
            sb.append("لطفاً متن نهایی را بر اساس این توضیح تولید کن و اگر اطلاعات بیشتری درباره برند یا تجربه کاربران در وب وجود دارد، استفاده کن.\n\n");
        } else {
            sb.append("نام محصول: ").append(safe(r.getName())).append("\n");
            if (r.getAttributes() != null && !r.getAttributes().isEmpty())
                sb.append("مشخصات و ویژگی‌ها: ").append(r.getAttributes()).append("\n");
            if (r.getAdditionalInfo() != null && !r.getAdditionalInfo().isEmpty())
                sb.append("اطلاعات تکمیلی: ").append(r.getAdditionalInfo()).append("\n");

            sb.append("\nلطفاً متن را حرفه‌ای، صادقانه و جذاب بنویس و اگر اطلاعاتی درباره برند یا تجربه کاربران در وب وجود دارد، استفاده کن.\n\n");
        }

        sb.append("قالب متن: رسمی، تا حد امکان کوتاه و قانع‌کننده، حداکثر 3 پاراگراف، نقاط قوت در bullet list.");
        return sb.toString();
    }

    private String safe(String s) {
        return s == null ? "-" : s;
    }
}
