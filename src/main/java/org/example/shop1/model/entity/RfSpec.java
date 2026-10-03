package org.example.shop1.model.entity;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * دادهٔ فنیِ رادیوییِ یک محصول — شیءِ {@code rf} در قراردادِ
 * {@code docs/dadehlink-api-contract.md}، بخشِ «نسخهٔ ۲».
 * <p>
 * عمومی است: هر فروشگاهی که آنتن یا رادیو می‌فروشد همین را لازم دارد.
 * <p>
 * 🔴 <b>خانهٔ خالی یعنی «منبع نداده»</b>، نه صفر. همهٔ عددها wrapper‌اند تا نال بمانند و
 * کلاینت «نامشخص» نشان دهد. صفرِ ساختگی بدترین حالت است: پیشنهادِ کالا با آن حساب
 * می‌شود و غلط ولی باورپذیر درمی‌آید.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RfSpec {

    public static final List<String> KINDS = List.of("ANTENNA", "RADIO", "RADIO_INTEGRATED");
    public static final List<String> ANTENNA_TYPES = List.of("DISH", "SECTOR", "OMNI", "PANEL", "GRID", "HORN", "OTHER");
    public static final List<String> POLARIZATIONS = List.of("SINGLE", "DUAL");

    private String kind;
    private List<Band> bands = new ArrayList<>();
    private Antenna antenna;
    private Radio radio;
    private List<Rate> rates = new ArrayList<>();
    private Source source;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Band {
        private Integer minMhz;
        private Integer maxMhz;
        public Band() {}
        public Band(Integer minMhz, Integer maxMhz) { this.minMhz = minMhz; this.maxMhz = maxMhz; }
        public boolean covers(double fMhz) {
            return minMhz != null && maxMhz != null && fMhz >= minMhz && fMhz <= maxMhz;
        }
        public Integer getMinMhz() { return minMhz; }
        public void setMinMhz(Integer minMhz) { this.minMhz = minMhz; }
        public Integer getMaxMhz() { return maxMhz; }
        public void setMaxMhz(Integer maxMhz) { this.maxMhz = maxMhz; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Antenna {
        private Double gainDbi;
        private String type;
        private Double diameterCm;
        private Double beamwidthDeg;
        private String polarization;
        public Double getGainDbi() { return gainDbi; }
        public void setGainDbi(Double gainDbi) { this.gainDbi = gainDbi; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Double getDiameterCm() { return diameterCm; }
        public void setDiameterCm(Double diameterCm) { this.diameterCm = diameterCm; }
        public Double getBeamwidthDeg() { return beamwidthDeg; }
        public void setBeamwidthDeg(Double beamwidthDeg) { this.beamwidthDeg = beamwidthDeg; }
        public String getPolarization() { return polarization; }
        public void setPolarization(String polarization) { this.polarization = polarization; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Radio {
        private Double txMaxDbm;
        private String txCond;
        private Double sensLowDbm;
        private String sensLowCond;
        private Double sensHighDbm;
        private String sensHighCond;
        private String compatFamily;
        public Double getTxMaxDbm() { return txMaxDbm; }
        public void setTxMaxDbm(Double txMaxDbm) { this.txMaxDbm = txMaxDbm; }
        public String getTxCond() { return txCond; }
        public void setTxCond(String txCond) { this.txCond = txCond; }
        public Double getSensLowDbm() { return sensLowDbm; }
        public void setSensLowDbm(Double sensLowDbm) { this.sensLowDbm = sensLowDbm; }
        public String getSensLowCond() { return sensLowCond; }
        public void setSensLowCond(String sensLowCond) { this.sensLowCond = sensLowCond; }
        public Double getSensHighDbm() { return sensHighDbm; }
        public void setSensHighDbm(Double sensHighDbm) { this.sensHighDbm = sensHighDbm; }
        public String getSensHighCond() { return sensHighCond; }
        public void setSensHighCond(String sensHighCond) { this.sensHighCond = sensHighCond; }
        public String getCompatFamily() { return compatFamily; }
        public void setCompatFamily(String compatFamily) { this.compatFamily = compatFamily; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Rate {
        private Integer minMhz;
        private Integer maxMhz;
        private Double channelMhz;
        private String rateLabel;
        private Double txDbm;
        private Double sensDbm;
        private Double rateMbps;
        public Integer getMinMhz() { return minMhz; }
        public void setMinMhz(Integer minMhz) { this.minMhz = minMhz; }
        public Integer getMaxMhz() { return maxMhz; }
        public void setMaxMhz(Integer maxMhz) { this.maxMhz = maxMhz; }
        public Double getChannelMhz() { return channelMhz; }
        public void setChannelMhz(Double channelMhz) { this.channelMhz = channelMhz; }
        public String getRateLabel() { return rateLabel; }
        public void setRateLabel(String rateLabel) { this.rateLabel = rateLabel; }
        public Double getTxDbm() { return txDbm; }
        public void setTxDbm(Double txDbm) { this.txDbm = txDbm; }
        public Double getSensDbm() { return sensDbm; }
        public void setSensDbm(Double sensDbm) { this.sensDbm = sensDbm; }
        public Double getRateMbps() { return rateMbps; }
        public void setRateMbps(Double rateMbps) { this.rateMbps = rateMbps; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Source {
        private String url;
        private String checked;
        public Source() {}
        public Source(String url, String checked) { this.url = url; this.checked = checked; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getChecked() { return checked; }
        public void setChecked(String checked) { this.checked = checked; }
    }

    public boolean coversMhz(double fMhz) {
        return bands != null && bands.stream().anyMatch(b -> b.covers(fMhz));
    }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public List<Band> getBands() { return bands; }
    public void setBands(List<Band> bands) { this.bands = bands; }
    public Antenna getAntenna() { return antenna; }
    public void setAntenna(Antenna antenna) { this.antenna = antenna; }
    public Radio getRadio() { return radio; }
    public void setRadio(Radio radio) { this.radio = radio; }
    public List<Rate> getRates() { return rates; }
    public void setRates(List<Rate> rates) { this.rates = rates; }
    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
}
