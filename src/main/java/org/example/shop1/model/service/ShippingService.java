package org.example.shop1.model.service;

import org.example.shop1.model.dto.ShippingOption;
import org.example.shop1.model.entity.Address;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * روشِ ارسال — یک گزینهٔ ثابتِ «پس‌کرایه توسطِ کارگزار»، بدونِ انتخاب از میانِ
 * چند شرکتِ حمل‌ونقل. تصمیمِ مالک (۲۰۲۶-۰۹-۱۷): کارگزار خودش روشِ ارسال را در
 * لحظهٔ تحویل تعیین می‌کند؛ سایت دیگر نامِ شرکتِ حمل‌ونقلِ خاصی (پیشتاز/تیپاکس) را
 * به مشتری پیشنهاد نمی‌دهد.
 */
@Service
public class ShippingService {

    public List<ShippingOption> calculateOptions(Address destination, Double totalWeight) {
        return List.of(new ShippingOption(
                "COURIER_COD",
                "کرایه حمل و نقل به صورت پس‌کرایه توسط کارگزار از مشتری دریافت می‌شود.",
                null));
    }
}
