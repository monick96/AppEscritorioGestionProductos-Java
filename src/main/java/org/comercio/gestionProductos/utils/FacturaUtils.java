package org.comercio.gestionProductos.utils;

import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import lombok.Data;
import org.comercio.gestionProductos.models.Producto;
import org.comercio.gestionProductos.models.ProductoEnCaja;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.prefs.Preferences;

@Data
public class FacturaUtils {

    private static final DecimalFormat decimalFormat = new DecimalFormat("$#,##0");

    private static final Preferences prefs = Preferences.userNodeForPackage(FacturaUtils.class);

    private static final String PREF_ULTIMA_CARPETA = "ultimaCarpetaSeleccionada";



    public static void generarPresupuestoPDF2(String nombreCliente,String local,List<ProductoEnCaja> productos, String logoPath, String filePath, boolean incluirSugerido) {
        try {
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            if (local == null || local.isEmpty()) {
                local = " ";
            }

            // Crear una tabla para alinear el logo y el título
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
            headerTable.setWidth(UnitValue.createPercentValue(100));
            headerTable.setBorder(new SolidBorder(1));
            headerTable.setMarginBottom(4); // margen entre tabla header y tabla productos

            // Logo
            ImageData imageData = ImageDataFactory.create(logoPath);
            Image logo = new Image(imageData);
            logo.setWidth(UnitValue.createPercentValue(40));
            Cell logoCell = new Cell().add(logo).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);
            logoCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
            headerTable.addCell(logoCell);

            // Fecha
            String fecha = new SimpleDateFormat("dd/MM/yyyy").format(new Date());

            // Título y número de factura

            Paragraph title = new Paragraph("Presupuesto (tu comercio)")
                    .setTextAlignment(TextAlignment.LEFT)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setBold()
                    .setFontSize(10.5f);
            title.setWidth(UnitValue.createPercentValue(50));
            // fecha y nombre del cliente
            // Crear un párrafo para la fecha y el cliente con diferentes estilo

            Paragraph fechaYCliente = new Paragraph();
            fechaYCliente.add(new Text("Fecha: ").setBold());
            fechaYCliente.add(new Text(fecha));
            fechaYCliente.add("\n");
            fechaYCliente.add(new Text("Cliente: ").setBold());
            fechaYCliente.add(new Text(nombreCliente));

            fechaYCliente.setTextAlignment(TextAlignment.LEFT)
                    .setFontSize(10f)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);

            Cell titleCell = new Cell().add(title).add(fechaYCliente).setBorder(Border.NO_BORDER);
            titleCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
            titleCell.setTextAlignment(TextAlignment.CENTER);
            headerTable.addCell(titleCell);


            // Agregar la tabla de encabezado al documento
            document.add(headerTable);

            // Tabla de productos
            // Definir las columnas de la tabla de productos según si se incluye "Precio Sugerido" o no
            float[] columnWidths = incluirSugerido ?
                    new float[]{3, 8, 2, 2, 2, 2, 2} : // Con "Precio Sugerido"
                    new float[]{3, 7, 2, 2, 2, 2};    // Sin "Precio Sugerido"

            Table table = new Table(UnitValue.createPercentArray(columnWidths))
                    .setWidth(UnitValue.createPercentValue(100));
            table.setBorder(Border.NO_BORDER);
            table.setTextAlignment(TextAlignment.CENTER);
            table.setVerticalAlignment(VerticalAlignment.MIDDLE);

            // Encabezado de la tabla
            List<String> headers = incluirSugerido ?
                    Arrays.asList("Marca", "Descripción", "Sabor", "Cantidad", "Precio Unitario", "SubTotal", "Precio Sugerido") :
                    Arrays.asList("Marca", "Descripción", "Sabor", "Cantidad", "Precio Unitario", "Total");

            // Encabezado de la tabla
            for (String header : headers) {
                Cell headerCell = new Cell().add(new Paragraph(header).setBold());
                headerCell.setBackgroundColor(ColorConstants.LIGHT_GRAY); // Fondo gris
                headerCell.setTextAlignment(TextAlignment.CENTER); // Alineación vertical centrada
                headerCell.setBorder(new SolidBorder(1)); // Bordes completos para las celdas del encabezado
                headerCell.setVerticalAlignment(VerticalAlignment.MIDDLE); // Alineación vertical centrada
                headerCell.setCharacterSpacing(0.6f); // Espaciado entre letras
                headerCell.setFontSize(8.5f);
                table.addHeaderCell(headerCell);
            }

            //cuerpo de tabla
            for (ProductoEnCaja productoEnCaja : productos) {
                Producto producto = productoEnCaja.getProducto();
                table.addCell(createStyledCell(new Paragraph(producto.getMarca())));
                table.addCell(createStyledCell(new Paragraph(producto.getDescripcion())));
                table.addCell(createStyledCell(new Paragraph(producto.getSabor())));
                table.addCell(createStyledCell(new Paragraph(String.valueOf(productoEnCaja.getCantidad()))));
                table.addCell(createStyledCell(new Paragraph(decimalFormat.format(producto.getPrecioFinal2()))));
                table.addCell(createStyledCell(new Paragraph(decimalFormat.format(producto.getPrecioFinal2() * productoEnCaja.getCantidad()))));

                if (incluirSugerido) {
                    table.addCell(createStyledCell(new Paragraph(decimalFormat.format(producto.getPrecioSugerido()))));

                }
            }

            // Agregar bordes exteriores a la tabla
            table.setBorderBottom(new SolidBorder(1));
            table.setBorderLeft(new SolidBorder(1));

            //agregar tabla al doc
            document.add(table);

            // Crear una tabla para el subtotal, total descuento y total
            Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{7,4}));
            totalsTable.setWidth(UnitValue.createPercentValue(100));
            totalsTable.setBorder(new SolidBorder(1));
            totalsTable.setMarginTop(4);

            // Local
            String localSeleccionado = local; // Cambiar por la variable que almacena el local seleccionado
            Paragraph localParagraph = new Paragraph()
                    .add(new Text("Local: ").setBold())
                    .add(new Text(localSeleccionado))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setFontSize(8)
                    .setBorder(Border.NO_BORDER)
                    .setMarginTop(1.5f);

            // Agregar el párrafo del local a la tabla en una nueva fila
            Cell localCell = new Cell()
                    .add(localParagraph)
                    .setBorder(Border.NO_BORDER)
                    //.setPaddingLeft(5)// Ajusta el valor según el margen deseado
                   // .setPaddingBottom(10) // Ajusta el valor para crear espacio en la parte superior
                  //.setVerticalAlignment(VerticalAlignment.BOTTOM); // Alinea el contenido en la parte inferior
                    .setPaddingLeft(5);
            totalsTable.addCell(localCell);

            // Subtotal
            double subtotal = getSubtotal2(productos);
            Paragraph subtotalParagraph = new Paragraph()
                    .add(new Text("Subtotal: ").setBold())
                    .add(new Text(decimalFormat.format(subtotal)))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setFontSize(9);

            // Total descuento
            double totalDescuento = getTotalDescuento(productos);
            Paragraph totalDescuentoParagraph = new Paragraph()
                    .add(new Text("Total Descuento: ").setBold())
                    .add(new Text(decimalFormat.format(totalDescuento)))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setFontSize(9);

            // Total
            double total =  getTotal2(productos);
            Paragraph totalParagraph = new Paragraph()
                    .add(new Text("Total: ").setBold())
                    .add(new Text(decimalFormat.format(total)))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setFontSize(9)
                    .setBorder(new SolidBorder(0.5f))
                    .setMarginTop(1.5f)
                    .setBold();

            // Agregar los párrafos a la tabla
            //Cell totalsCell = new Cell().add(subtotalParagraph).add(totalDescuentoParagraph).add(totalParagraph).setBorder(Border.NO_BORDER);
            Cell totalsCell = new Cell().add(totalParagraph).setBorder(Border.NO_BORDER);
            totalsTable.addCell(totalsCell);

            // Agregar la tabla de totales al documento
            document.add(totalsTable);
            document.close();
            System.out.println("Presupuesto creada en " + filePath);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void generarPresupuestoPDF1(String nombreCliente,String local,List<ProductoEnCaja> productos, String logoPath, String filePath, boolean incluirSugerido) {
        try {
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            if (local == null || local.isEmpty()) {
                local = " ";
            }

            // Crear una tabla para alinear el logo y el título
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
            headerTable.setWidth(UnitValue.createPercentValue(100));
            headerTable.setBorder(new SolidBorder(1));
            headerTable.setMarginBottom(4); // margen entre tabla header y tabla productos

            // Logo
            ImageData imageData = ImageDataFactory.create(logoPath);
            Image logo = new Image(imageData);
            logo.setWidth(UnitValue.createPercentValue(40));
            Cell logoCell = new Cell().add(logo).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);
            logoCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
            headerTable.addCell(logoCell);

            // Fecha
            String fecha = new SimpleDateFormat("dd/MM/yyyy").format(new Date());

            // Título y número de factura

            Paragraph title = new Paragraph("Presupuesto (tu comercio))")
                    .setTextAlignment(TextAlignment.LEFT)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setBold()
                    .setFontSize(10.5f);
            title.setWidth(UnitValue.createPercentValue(50));
            // fecha y nombre del cliente
            // Crear un párrafo para la fecha y el cliente con diferentes estilo

            Paragraph fechaYCliente = new Paragraph();
            fechaYCliente.add(new Text("Fecha: ").setBold());
            fechaYCliente.add(new Text(fecha));
            fechaYCliente.add("\n");
            fechaYCliente.add(new Text("Cliente: ").setBold());
            fechaYCliente.add(new Text(nombreCliente));

            fechaYCliente.setTextAlignment(TextAlignment.LEFT)
                    .setFontSize(10f)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);

            Cell titleCell = new Cell().add(title).add(fechaYCliente).setBorder(Border.NO_BORDER);
            titleCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
            titleCell.setTextAlignment(TextAlignment.CENTER);
            headerTable.addCell(titleCell);


            // Agregar la tabla de encabezado al documento
            document.add(headerTable);

            // Tabla de productos
            //Table table = new Table(UnitValue.createPercentArray(new float[]{3, 8, 2, 2, 2, 2, 2}))
            //.setWidth(UnitValue.createPercentValue(100));
            // Definir las columnas de la tabla de productos según si se incluye "Precio Sugerido" o no
            float[] columnWidths = incluirSugerido ?
                    new float[]{3, 8, 2, 2, 2, 2, 2} : // Con "Precio Sugerido"
                    new float[]{3, 7, 2, 2, 2, 2};    // Sin "Precio Sugerido"

            Table table = new Table(UnitValue.createPercentArray(columnWidths))
                    .setWidth(UnitValue.createPercentValue(100));
            table.setBorder(Border.NO_BORDER);
            table.setTextAlignment(TextAlignment.CENTER);
            table.setVerticalAlignment(VerticalAlignment.MIDDLE);

            // Encabezado de la tabla
            List<String> headers = incluirSugerido ?
                    Arrays.asList("Marca", "Descripción", "Sabor", "Cantidad", "Precio Unitario", "SubTotal", "Precio Sugerido") :
                    Arrays.asList("Marca", "Descripción", "Sabor", "Cantidad", "Precio Unitario", "Total");

            // Encabezado de la tabla
            for (String header : headers) {
                Cell headerCell = new Cell().add(new Paragraph(header).setBold());
                headerCell.setBackgroundColor(ColorConstants.LIGHT_GRAY); // Fondo gris
                headerCell.setTextAlignment(TextAlignment.CENTER); // Alineación vertical centrada
                headerCell.setBorder(new SolidBorder(1)); // Bordes completos para las celdas del encabezado
                headerCell.setVerticalAlignment(VerticalAlignment.MIDDLE); // Alineación vertical centrada
                headerCell.setCharacterSpacing(0.6f); // Espaciado entre letras
                headerCell.setFontSize(8.5f);
                table.addHeaderCell(headerCell);
            }

            //cuerpo de tabla
            for (ProductoEnCaja productoEnCaja : productos) {
                Producto producto = productoEnCaja.getProducto();
                table.addCell(createStyledCell(new Paragraph(producto.getMarca())));
                table.addCell(createStyledCell(new Paragraph(producto.getDescripcion())));
                table.addCell(createStyledCell(new Paragraph(producto.getSabor())));
                table.addCell(createStyledCell(new Paragraph(String.valueOf(productoEnCaja.getCantidad()))));
                table.addCell(createStyledCell(new Paragraph(decimalFormat.format(producto.getPrecioMayorista1()))));
                table.addCell(createStyledCell(new Paragraph(decimalFormat.format(producto.getPrecioMayorista1() * productoEnCaja.getCantidad()))));

                if (incluirSugerido) {

                    table.addCell(createStyledCell(new Paragraph(decimalFormat.format(producto.getPrecioSugerido()))));

                }
            }

            // Agregar bordes exteriores a la tabla
            table.setBorderBottom(new SolidBorder(1));
            table.setBorderLeft(new SolidBorder(1));

            //agregar tabla al doc
            document.add(table);

            // Crear una tabla para el subtotal, total descuento y total
            Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{7,4}));
            totalsTable.setWidth(UnitValue.createPercentValue(100));
            totalsTable.setBorder(new SolidBorder(1));
            totalsTable.setMarginTop(4);

            // Local
            String localSeleccionado = local; // Cambiar por la variable que almacena el local seleccionado
            Paragraph localParagraph = new Paragraph()
                    .add(new Text("Local: ").setBold())
                    .add(new Text(localSeleccionado))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setFontSize(8)
                    .setBorder(Border.NO_BORDER)
                    .setMarginTop(1.5f);

            // Agregar el párrafo del local a la tabla en una nueva fila
            Cell localCell = new Cell()
                    .add(localParagraph)
                    .setBorder(Border.NO_BORDER)
                    .setPaddingLeft(5); // Ajusta el valor según el margen deseado
            totalsTable.addCell(localCell);

            // Total
            double total =  getTotal1(productos);
            Paragraph totalParagraph = new Paragraph()
                    .add(new Text("Total: ").setBold())
                    .add(new Text(decimalFormat.format(total)))
                    .setTextAlignment(TextAlignment.LEFT)
                    .setFontSize(9)
                    .setBorder(new SolidBorder(0.5f))
                    .setMarginTop(1.5f)
                    .setBold();

            // Agregar los párrafos a la tabla
            Cell totalsCell = new Cell().add(totalParagraph).setBorder(Border.NO_BORDER);
            totalsTable.addCell(totalsCell);

            // Agregar la tabla de totales al documento
            document.add(totalsTable);
            document.close();
            System.out.println("Presupuesto creada en " + filePath);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public  static double getTotal1(List<ProductoEnCaja> productos){
        double total = productos.stream()
                .mapToDouble(p -> p.getProducto().getPrecioMayorista1() * p.getCantidad())
                .sum();
        return total;
    }

    public  static double getTotal2(List<ProductoEnCaja> productos){
        double total = productos.stream()
                .mapToDouble(p -> p.getProducto().getPrecioFinal2() * p.getCantidad())
                .sum();
        return total;
    }
    public  static double getTotalDescuento(List<ProductoEnCaja> productos){
        double totalDescuento = productos.stream()
                .mapToDouble(p -> (p.getProducto().getMontoDescuento2() * p.getCantidad()))
                .sum();
        return totalDescuento;
    }

    private static double getSubtotal2(List<ProductoEnCaja> productos) {
        double subtotal = productos.stream()
                .mapToDouble(p -> p.getProducto().getPrecioMayorista2() * p.getCantidad())
                .sum();
        return subtotal;
    }

    public static String seleccionarUbicacionYNombreArchivo2(Stage primaryStage, int numeroFactura) {
        Date fecha = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH.mm");
        String formattedDate = formatter.format(fecha);
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Presupuesto");
        fileChooser.setInitialFileName("presupuesto2_" + numeroFactura + "_" + formattedDate + ".pdf");

        // Recuperar la última carpeta seleccionada desde las preferencias
        String ultimaCarpetaSeleccionada = prefs.get(PREF_ULTIMA_CARPETA, null);
        if (ultimaCarpetaSeleccionada != null) {
            File initialDir = new File(ultimaCarpetaSeleccionada);
            if (initialDir.exists()) {
                fileChooser.setInitialDirectory(initialDir);
            }

            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File selectedFile = fileChooser.showSaveDialog(primaryStage);

            if (selectedFile != null) {
                // Guardar la ruta de la carpeta seleccionada en las preferencias
                prefs.put(PREF_ULTIMA_CARPETA, selectedFile.getParent());
                return selectedFile.getAbsolutePath();
            }

        }
        return null;
    }

    public static String seleccionarUbicacionYNombreArchivo(Stage primaryStage, int numeroFactura) {
        Date fecha = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH.mm");
        String formattedDate = formatter.format(fecha);
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Presupuesto");
        fileChooser.setInitialFileName("presupuesto1_" + numeroFactura + "_" + formattedDate + ".pdf");

        // Recuperar la última carpeta seleccionada desde las preferencias
        String ultimaCarpetaSeleccionada = prefs.get(PREF_ULTIMA_CARPETA, null);
        if (ultimaCarpetaSeleccionada != null) {
            File initialDir = new File(ultimaCarpetaSeleccionada);
            if (initialDir.exists()) {
                fileChooser.setInitialDirectory(initialDir);
            }
        }

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File selectedFile = fileChooser.showSaveDialog(primaryStage);

        if (selectedFile != null) {
            // Guardar la ruta de la carpeta seleccionada en las preferencias
            prefs.put(PREF_ULTIMA_CARPETA, selectedFile.getParent());
            return selectedFile.getAbsolutePath();
        } else {
            // Manejar el caso cuando no se seleccionó un archivo
            System.out.println("No se seleccionó un archivo.");
            return null;
        }

    }

        private static Cell createStyledCell (Paragraph content){
            return new Cell()
                    .add(content)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setFontSize(7.5f);
        }

    }


