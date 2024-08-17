package org.comercio.gestionProductos.models;
import lombok.*;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
@NoArgsConstructor
public class Producto {
    @NonNull
    private String codigo;
    private String marca;
    private String sabor;
    private String descripcion;
    @NonNull
    private double precioBase;//clasic en excel
    private double precioSugerido; //precio base por porcentaje especifico
    private double porcentajePrecioSugerido;
    private double precioMayorista1;//margen ganancia base * porcentaje //defecto
    @NonNull
    private double porcentajePrecioMayorista1;//porcentaje aplicado sobre el base para obtener precioventa
    private double precioMayorista2;//precio base menos porcentaje especifico/precio mayorista2
    @NonNull
    private double porcentajePrecioMayorista2;
    private double precioMayorista3;
    private double porcentajePrecioMayorista3;
    private double precioConDescuento2 = 0;
    private double montoDescuento2 = 0;
    private double porcentajeDescuento2;

    //metodos
    public void calcularPrecios() {

        precioMayorista1 = precioBase + ((precioBase * porcentajePrecioMayorista1) / 100);
        precioMayorista2 = precioBase + (precioBase * porcentajePrecioMayorista2 /100);
        precioMayorista3 = precioBase + (precioBase * porcentajePrecioMayorista3 /100);

    }

    public void calcularSugerido() {

        this.precioSugerido = getPrecioBase() + (getPrecioBase() * this.porcentajePrecioSugerido/100);

    }

    /***
     * Calcular precios de productos
     * calcula los precios cuando se carga del excel
     */
    public void calcularPreciosMasSugerido() {

        calcularPrecios();
        calcularSugerido();
        if (this.porcentajeDescuento2 > 0) {
            calcularPrecioConDescuento();
            calcularMontoDescuento();
        }

    }
    /**
     *Aplicar descuento al monto mayorista 2
     *  en vez de aplicar el porcentaje normal aplicamos el porcentaje
     *  de descuento de la columna que deberia ser menor
     * los montos son 2 , los numero de de forma ascendente por el monto
     * los montos se leen desde el excel y se guardan en el main como atributos
     * siendo 1 el mas bajo y 2 el mas alto

     */
    private void calcularPrecioConDescuento() {

        this.precioConDescuento2 = precioBase + ((precioBase * this.porcentajeDescuento2) / 100);

    }
    private void calcularMontoDescuento() {

        this.montoDescuento2 = this.precioMayorista2 - this.precioConDescuento2;

    }

    public double getPrecioFinal2() {
        return (this.precioConDescuento2 != 0) ? this.precioConDescuento2 : precioMayorista2;
    }

    public double calcularGananciaProducto2(){
        double precioFinal = getPrecioFinal2();
        return (precioFinal - precioBase);
    }

    public double calcularGananciaProducto1(){
        return (precioMayorista1 - precioBase);
    }


}
