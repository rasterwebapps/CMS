package com.cms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "role_dashboard_widget_configs")
public class RoleDashboardWidgetConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private AppRole role;

    @Column(name = "widget_key", nullable = false, length = 100)
    private String widgetKey;

    @Column(name = "widget_order", nullable = false)
    private int widgetOrder;

    @Column(name = "col_span", nullable = false)
    private int colSpan = 1;

    @Column(name = "row_span", nullable = false)
    private int rowSpan = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", columnDefinition = "jsonb")
    private String configJson;

    public RoleDashboardWidgetConfig() {}

    public RoleDashboardWidgetConfig(AppRole role, String widgetKey, int widgetOrder,
                                     int colSpan, int rowSpan) {
        this.role        = role;
        this.widgetKey   = widgetKey;
        this.widgetOrder = widgetOrder;
        this.colSpan     = colSpan;
        this.rowSpan     = rowSpan;
    }

    public Long getId()                     { return id; }
    public AppRole getRole()               { return role; }
    public String getWidgetKey()           { return widgetKey; }
    public int getWidgetOrder()            { return widgetOrder; }
    public int getColSpan()               { return colSpan; }
    public int getRowSpan()               { return rowSpan; }
    public String getConfigJson()         { return configJson; }

    public void setRole(AppRole role)             { this.role = role; }
    public void setWidgetKey(String widgetKey)    { this.widgetKey = widgetKey; }
    public void setWidgetOrder(int widgetOrder)   { this.widgetOrder = widgetOrder; }
    public void setColSpan(int colSpan)           { this.colSpan = colSpan; }
    public void setRowSpan(int rowSpan)           { this.rowSpan = rowSpan; }
    public void setConfigJson(String configJson)  { this.configJson = configJson; }
}
