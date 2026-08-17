package io.github.ramiro.escapesj.modelo;

import java.math.BigDecimal;

public class OperacionHistorica {
    private int id;
    private String fecha;
    private String referenciaPapel;
    private String cliente;
    private String descripcion;
    private BigDecimal importeTotal;
    private BigDecimal costoMateriales; // Puede ser null
    private String observaciones;
    private String estado; // 'PENDIENTE' o 'DIGITALIZADO'
    private Integer boletaDigitalId; // Puede ser null
    private String creadoEn;
    private String actualizadoEn;

    public OperacionHistorica() {
    }

    public OperacionHistorica(int id, String fecha, String referenciaPapel, String cliente, String descripcion,
                              BigDecimal importeTotal, BigDecimal costoMateriales, String observaciones,
                              String estado, Integer boletaDigitalId, String creadoEn, String actualizadoEn) {
        this.id = id;
        this.fecha = fecha;
        this.referenciaPapel = referenciaPapel;
        this.cliente = cliente;
        this.descripcion = descripcion;
        this.importeTotal = importeTotal;
        this.costoMateriales = costoMateriales;
        this.observaciones = observaciones;
        this.estado = estado;
        this.boletaDigitalId = boletaDigitalId;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getReferenciaPapel() { return referenciaPapel; }
    public void setReferenciaPapel(String referenciaPapel) { this.referenciaPapel = referenciaPapel; }

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getImporteTotal() { return importeTotal; }
    public void setImporteTotal(BigDecimal importeTotal) { this.importeTotal = importeTotal; }

    public BigDecimal getCostoMateriales() { return costoMateriales; }
    public void setCostoMateriales(BigDecimal costoMateriales) { this.costoMateriales = costoMateriales; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Integer getBoletaDigitalId() { return boletaDigitalId; }
    public void setBoletaDigitalId(Integer boletaDigitalId) { this.boletaDigitalId = boletaDigitalId; }

    public String getCreadoEn() { return creadoEn; }
    public void setCreadoEn(String creadoEn) { this.creadoEn = creadoEn; }

    public String getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(String actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
