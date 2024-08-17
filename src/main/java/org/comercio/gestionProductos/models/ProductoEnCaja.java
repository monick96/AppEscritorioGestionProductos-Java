package org.comercio.gestionProductos.models;

import lombok.Data;

@Data
public class ProductoEnCaja {
    private Producto producto;
    private int cantidad;
    private double total1;
    private double total2;

    public ProductoEnCaja(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.total1 = calcularTotal1();
        this.total2 = calcularTotal2();
    }

    public double calcularTotal1() {
        return this.cantidad * this.producto.getPorcentajePrecioMayorista1();
    }

    public double calcularTotal2() {
        return this.cantidad * this.producto.getPrecioFinal2();
    }
    public double calcularTotalGanancia2() {
       return this.producto.calcularGananciaProducto2() * cantidad;
    }
    public double calcularTotalGanancia1() {
        return this.producto.calcularGananciaProducto1() * cantidad;
    }



}
