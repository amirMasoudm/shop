package org.example.shop1.model.service.util;

import java.util.HashMap;
import java.util.Map;

public class IranCityMapper {

    private static final Map<String, Integer> PROVINCE_IDS = new HashMap<>();
    private static final Map<String, Integer> CITY_IDS = new HashMap<>();

    static {
        PROVINCE_IDS.put("همدان", 1);
        PROVINCE_IDS.put("آذربایجان غربی", 2);
        PROVINCE_IDS.put("آذربایجان شرقی", 3);
        PROVINCE_IDS.put("اصفهان", 4);
        PROVINCE_IDS.put("ایلام", 5);
        PROVINCE_IDS.put("بوشهر", 6);
        PROVINCE_IDS.put("چهارمحال و بختیاری", 7);
        PROVINCE_IDS.put("تهران", 8);
        PROVINCE_IDS.put("مرکزی", 9);
        PROVINCE_IDS.put("فارس", 10);
        PROVINCE_IDS.put("خراسان رضوی", 11);
        PROVINCE_IDS.put("زنجان", 12);
        PROVINCE_IDS.put("سمنان", 13);
        PROVINCE_IDS.put("سیستان و بلوچستان", 14);
        PROVINCE_IDS.put("خوزستان", 15);
        PROVINCE_IDS.put("کردستان", 16);
        PROVINCE_IDS.put("کرمانشاه", 17);
        PROVINCE_IDS.put("کهگیلویه و بویراحمد", 18);
        PROVINCE_IDS.put("گیلان", 19);
        PROVINCE_IDS.put("لرستان", 20);
        PROVINCE_IDS.put("مازندران", 21);
        PROVINCE_IDS.put("کرمان", 22);
        PROVINCE_IDS.put("هرمزگان", 23);
        PROVINCE_IDS.put("اردبیل", 24);
        PROVINCE_IDS.put("قم", 25);
        PROVINCE_IDS.put("گلستان", 26);
        PROVINCE_IDS.put("قزوین", 27);
        PROVINCE_IDS.put("خراسان شمالی", 28);
        PROVINCE_IDS.put("خراسان جنوبی", 29);
        PROVINCE_IDS.put("البرز", 30);
        PROVINCE_IDS.put("یزد", 31);

        CITY_IDS.put("همدان", 101);
        CITY_IDS.put("ارومیه", 201);
        CITY_IDS.put("تبریز", 301);
        CITY_IDS.put("اصفهان", 401);
        CITY_IDS.put("ایلام", 501);
        CITY_IDS.put("بوشهر", 601);
        CITY_IDS.put("شهرکرد", 701);
        CITY_IDS.put("تهران", 801);
        CITY_IDS.put("اراک", 901);
        CITY_IDS.put("شیراز", 1001);
        CITY_IDS.put("مشهد", 1101);
        CITY_IDS.put("زنجان", 1201);
        CITY_IDS.put("سمنان", 1301);
        CITY_IDS.put("زاهدان", 1401);
        CITY_IDS.put("اهواز", 1501);
        CITY_IDS.put("سنندج", 1601);
        CITY_IDS.put("کرمانشاه", 1701);
        CITY_IDS.put("یاسوج", 1801);
        CITY_IDS.put("رشت", 1901);
        CITY_IDS.put("خرم آباد", 2001);
        CITY_IDS.put("ساری", 2101);
        CITY_IDS.put("کرمان", 2201);
        CITY_IDS.put("بندرعباس", 2301);
        CITY_IDS.put("اردبیل", 2401);
        CITY_IDS.put("قم", 2501);
        CITY_IDS.put("گرگان", 2601);
        CITY_IDS.put("قزوین", 2701);
        CITY_IDS.put("بجنورد", 2801);
        CITY_IDS.put("بیرجند", 2901);
        CITY_IDS.put("کرج", 3001);
        CITY_IDS.put("یزد", 3101);
    }

    public static Integer getProvinceId(String provinceName) {
        if (provinceName == null) return null;
        String clean = provinceName.replace("استان", "").trim();
        return PROVINCE_IDS.get(clean);
    }

    public static Integer getCityId(String cityName) {
        if (cityName == null) return null;
        return CITY_IDS.get(cityName.trim());
    }
}