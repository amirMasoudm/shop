package org.example.shop1.model.dto;

import java.util.List;

public class FilterResponseDto {
    private String filterKey; // مثلا "رنگ"
    private List<FilterOption> options; // مثلا ["قرمز (10)", "آبی (5)"]

    public FilterResponseDto(String filterKey, List<FilterOption> options) {
        this.filterKey = filterKey;
        this.options = options;
    }

    public String getFilterKey() { return filterKey; }
    public List<FilterOption> getOptions() { return options; }

    public static class FilterOption {
        private String value; // "قرمز"
        private long count;   // 10

        public FilterOption(String value, long count) {
            this.value = value;
            this.count = count;
        }

        public String getValue() { return value; }
        public long getCount() { return count; }
    }
}