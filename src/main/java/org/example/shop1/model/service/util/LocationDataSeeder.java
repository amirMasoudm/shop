package org.example.shop1.model.service.util;

import org.example.shop1.model.entity.IranCity;
import org.example.shop1.model.entity.IranProvince;
import org.example.shop1.model.reposritory.IranCityRepository;
import org.example.shop1.model.reposritory.IranProvinceRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class LocationDataSeeder {

    private final IranProvinceRepository provinceRepo;
    private final IranCityRepository cityRepo;

    public LocationDataSeeder(IranProvinceRepository provinceRepo, IranCityRepository cityRepo) {
        this.provinceRepo = provinceRepo;
        this.cityRepo = cityRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedLocationData() {
        // ۱. مقداردهی تمامی ۳۱ استان ایران با کدهای مصوب شرکت پست
        if (provinceRepo.count() == 0) {
            provinceRepo.saveAll(Arrays.asList(
                    new IranProvince("1", "همدان"),
                    new IranProvince("2", "آذربایجان غربی"),
                    new IranProvince("3", "آذربایجان شرقی"),
                    new IranProvince("4", "اصفهان"),
                    new IranProvince("5", "ایلام"),
                    new IranProvince("6", "بوشهر"),
                    new IranProvince("7", "چهارمحال و بختیاری"),
                    new IranProvince("8", "تهران"),
                    new IranProvince("9", "مرکزی"),
                    new IranProvince("10", "فارس"),
                    new IranProvince("11", "خراسان رضوی"),
                    new IranProvince("12", "زنجان"),
                    new IranProvince("13", "سمنان"),
                    new IranProvince("14", "سیستان و بلوچستان"),
                    new IranProvince("15", "خوزستان"),
                    new IranProvince("16", "کردستان"),
                    new IranProvince("17", "کرمانشاه"),
                    new IranProvince("18", "کهگیلویه و بویراحمد"),
                    new IranProvince("19", "گیلان"),
                    new IranProvince("20", "لرستان"),
                    new IranProvince("21", "مازندران"),
                    new IranProvince("22", "کرمان"),
                    new IranProvince("23", "هرمزگان"),
                    new IranProvince("24", "اردبیل"),
                    new IranProvince("25", "قم"),
                    new IranProvince("26", "گلستان"),
                    new IranProvince("27", "قزوین"),
                    new IranProvince("28", "خراسان شمالی"),
                    new IranProvince("29", "خراسان جنوبی"),
                    new IranProvince("30", "البرز"),
                    new IranProvince("31", "یزد")
            ));
        }

        // ۲. مقداردهی مراکز هر ۳۱ استان با کدهای مصوب شرکت پست
        if (cityRepo.count() == 0) {
            cityRepo.saveAll(Arrays.asList(
                    new IranCity("101", "همدان", "1"),
                    new IranCity("201", "ارومیه", "2"),
                    new IranCity("301", "تبریز", "3"),
                    new IranCity("401", "اصفهان", "4"),
                    new IranCity("501", "ایلام", "5"),
                    new IranCity("601", "بوشهر", "6"),
                    new IranCity("701", "شهرکرد", "7"),
                    new IranCity("801", "تهران", "8"),
                    new IranCity("901", "اراک", "9"),
                    new IranCity("1001", "شیراز", "10"),
                    new IranCity("1101", "مشهد", "11"),
                    new IranCity("1201", "زنجان", "12"),
                    new IranCity("1301", "سمنان", "13"),
                    new IranCity("1401", "زاهدان", "14"),
                    new IranCity("1501", "اهواز", "15"),
                    new IranCity("1601", "سنندج", "16"),
                    new IranCity("1701", "کرمانشاه", "17"),
                    new IranCity("1801", "یاسوج", "18"),
                    new IranCity("1901", "رشت", "19"),
                    new IranCity("2001", "خرم آباد", "20"),
                    new IranCity("2101", "ساری", "21"),
                    new IranCity("2201", "کرمان", "22"),
                    new IranCity("2301", "بندرعباس", "23"),
                    new IranCity("2401", "اردبیل", "24"),
                    new IranCity("2501", "قم", "25"),
                    new IranCity("2601", "گرگان", "26"),
                    new IranCity("2701", "قزوین", "27"),
                    new IranCity("2801", "بجنورد", "28"),
                    new IranCity("2901", "بیرجند", "29"),
                    new IranCity("3001", "کرج", "30"),
                    new IranCity("3101", "یزد", "31")
            ));
        }
    }
}