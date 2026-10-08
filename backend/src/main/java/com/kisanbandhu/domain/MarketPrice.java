package com.kisanbandhu.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_prices", indexes = @Index(name = "idx_price_commodity_date", columnList = "commodity,price_date"))
public class MarketPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String commodity;

    @Column(name = "mandi_name", length = 120)
    private String mandiName;

    @Column(length = 80)
    private String state;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false, length = 12)
    private String unit = "quintal";

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(nullable = false, length = 16)
    private String source = "AGMARKNET";

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCommodity() { return commodity; }
    public void setCommodity(String commodity) { this.commodity = commodity; }
    public String getMandiName() { return mandiName; }
    public void setMandiName(String mandiName) { this.mandiName = mandiName; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public LocalDate getPriceDate() { return priceDate; }
    public void setPriceDate(LocalDate priceDate) { this.priceDate = priceDate; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime fetchedAt) { this.fetchedAt = fetchedAt; }
}
