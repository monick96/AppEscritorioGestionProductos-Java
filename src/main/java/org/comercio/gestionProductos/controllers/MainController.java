package org.comercio.gestionProductos.controllers;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.Data;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.comercio.gestionProductos.models.ProductoEnCaja;
import org.comercio.gestionProductos.utils.FacturaUtils;
import org.comercio.gestionProductos.utils.NotificationManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

@Data
public class MainController {

        private static int numeroFactura;

        private final String numeroFacturaFilePath = "numero_factura.txt";

        private List<String> locales;

        private List<ProductoEnCaja> productosEnCaja;

        private static final String PREFS_KEY = "rutaExcelPresupuesto";

        private Preferences prefs = Preferences.userNodeForPackage(MainController.class);

        public MainController(List<String> locales, List<ProductoEnCaja> productosEnCaja) {
                this.locales = locales;
                this.productosEnCaja = productosEnCaja;
                cargarNumeroFactura();
        }

        //generar factura
        public void mostrarPresupuesto(Stage primaryStage) {

                if (productosEnCaja.isEmpty()) {
                        // Mostrar una alerta informando que no hay productos
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("No se puede generar el presupuesto");
                        alert.setHeaderText(null);
                        alert.setContentText("No hay productos en la lista para generar un presupuesto.");
                        alert.showAndWait();
                        return; // Salir del método para evitar generar el presupuesto
                }

                numeroFactura++;
                guardarNumeroFactura(); // Guardar el número de factura actualizado

                // Crear un Dialog personalizado
                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("Datos del Presupuesto");

                // Campo de texto para el nombre del cliente
                TextField nameField = new TextField();
                nameField.setPromptText("Nombre del Cliente");
                nameField.setPrefHeight(35); // Ajusta la altura del campo de texto
                //nameField.setMaxWidth(250); // Ajusta el ancho máximo del campo de texto

                // ComboBox para la selección del local
                ComboBox<String> comboBoxLocales = new ComboBox<>();
                comboBoxLocales.getItems().addAll(locales);
                comboBoxLocales.setPromptText("Seleccione el Local");
                comboBoxLocales.setPrefHeight(35); // Ajusta la altura del ComboBox
                //comboBoxLocales.setMaxWidth(250); // Ajusta el ancho máximo del ComboBox

                // CheckBox para incluir o no la columna "Precio Sugerido"
                CheckBox includePriceSuggestedCheckBox = new CheckBox("Incluir columna Precio Sugerido");
                includePriceSuggestedCheckBox.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a9d8f;");

                // Contenedor VBox para organizar los componentes
                VBox vbox = new VBox();
                vbox.setSpacing(10);
                vbox.setPadding(new Insets(20, 20, 20, 20));
                vbox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #2a9d8f; -fx-border-width: 2px; -fx-border-radius: 5px;");
                vbox.getChildren().addAll(
                        new Label("Nombre del Cliente:"),
                        nameField,
                        new Label("Seleccione el Local:"),
                        comboBoxLocales,
                        includePriceSuggestedCheckBox
                );

                // Establecer el contenido del diálogo
                // Configurar el contenido del diálogo
                DialogPane dialogPane = dialog.getDialogPane();
                dialogPane.setContent(vbox);
                dialogPane.setPadding(new Insets(20, 20, 20, 20)); // Ajusta el padding del DialogPane

                // Botones del diálogo
                ButtonType okButton = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
                ButtonType cancelButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
                dialog.getDialogPane().getButtonTypes().setAll(okButton, cancelButton);

                // Arreglo para almacenar la información del cliente y si se debe incluir "Precio Sugerido"
                final String[] customerInfo = new String[3];
                customerInfo[0] = " "; // Nombre del cliente por defecto
                customerInfo[1] = ""; // Local por defecto (vacío)
                customerInfo[2] = "false"; // Incluir Precio Sugerido por defecto (no)

                // Mostrar el diálogo y manejar el resultado
                dialog.showAndWait().ifPresent(buttonType -> {
                        if (buttonType == okButton) {
                                customerInfo[0] = nameField.getText().isEmpty() ? " " : nameField.getText();
                                customerInfo[1] = comboBoxLocales.getValue();
                                customerInfo[2] = Boolean.toString(includePriceSuggestedCheckBox.isSelected());

                                String nombreCliente = customerInfo[0];
                                String localSeleccionado = customerInfo[1];
                                boolean incluirPrecioSugerido = Boolean.parseBoolean(customerInfo[2]);


                                String logoPath = getLogoPath(); // Ruta del logo

                                // Seleccionar ubicación y nombre del archivo
                                String savePath = FacturaUtils.seleccionarUbicacionYNombreArchivo(primaryStage, numeroFactura);

                                // Generar la factura en PDF
                                FacturaUtils.generarPresupuestoPDF1(nombreCliente, localSeleccionado, productosEnCaja, logoPath, savePath, incluirPrecioSugerido);

                                Platform.runLater(() -> {

                                        //notificacion pdf guardado
                                        NotificationManager.showNotification("El archivo PDF se guardó correctamente en:\n" + savePath, primaryStage);

                                        new Thread(() -> generarExcelPresupuesto1(primaryStage, nombreCliente, localSeleccionado, productosEnCaja)).start();

                                });

                        }
                });

        }

        public void mostrarPresupuesto2(Stage primaryStage) {

                if (productosEnCaja.isEmpty()) {
                        // Mostrar una alerta informando que no hay productos
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("No se puede generar el presupuesto");
                        alert.setHeaderText(null);
                        alert.setContentText("No hay productos en la lista para generar un presupuesto.");
                        alert.showAndWait();
                        return; // Salir del método para evitar generar el presupuesto
                }

                numeroFactura++;
                guardarNumeroFactura(); // Guardar el número de factura actualizado

                // Crear un Dialog personalizado
                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("Datos del Presupuesto");

                // Campo de texto para el nombre del cliente
                TextField nameField = new TextField();
                nameField.setPromptText("Nombre del Cliente");
                nameField.setPrefHeight(35); // Ajusta la altura del campo de texto

                // ComboBox para la selección del local
                ComboBox<String> comboBoxLocales = new ComboBox<>();
                comboBoxLocales.getItems().addAll(locales);
                comboBoxLocales.setPromptText("Seleccione el Local");
                comboBoxLocales.setPrefHeight(35); // Ajusta la altura del ComboBox
                // CheckBox para incluir o no la columna "Precio Sugerido"
                CheckBox includePriceSuggestedCheckBox = new CheckBox("Incluir columna Precio Sugerido");
                includePriceSuggestedCheckBox.setStyle("-fx-font-weight: bold; -fx-text-fill: #2a9d8f;");

                // Contenedor VBox para organizar los componentes
                VBox vbox = new VBox();
                vbox.setSpacing(10);
                vbox.setPadding(new Insets(20, 20, 20, 20));
                vbox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #2a9d8f; -fx-border-width: 2px; -fx-border-radius: 5px;");
                vbox.getChildren().addAll(
                        new Label("Nombre del Cliente:"),
                        nameField,
                        new Label("Seleccione el Local:"),
                        comboBoxLocales,
                        includePriceSuggestedCheckBox
                );

                // Establecer el contenido del diálogo
                // Configurar el contenido del diálogo
                DialogPane dialogPane = dialog.getDialogPane();
                dialogPane.setContent(vbox);
                dialogPane.setPadding(new Insets(20, 20, 20, 20)); // Ajusta el padding del DialogPane

                // Botones del diálogo
                ButtonType okButton = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
                ButtonType cancelButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
                dialog.getDialogPane().getButtonTypes().setAll(okButton, cancelButton);

                // Arreglo para almacenar la información del cliente y si se debe incluir "Precio Sugerido"
                final String[] customerInfo = new String[3];
                customerInfo[0] = " "; // Nombre del cliente por defecto
                customerInfo[1] = ""; // Local por defecto (vacío)
                customerInfo[2] = "false"; // Incluir Precio Sugerido por defecto (no)

                // Mostrar el diálogo y manejar el resultado
                dialog.showAndWait().ifPresent(buttonType -> {
                        if (buttonType == okButton) {
                                customerInfo[0] = nameField.getText().isEmpty() ? " " : nameField.getText();
                                customerInfo[1] = comboBoxLocales.getValue();
                                customerInfo[2] = Boolean.toString(includePriceSuggestedCheckBox.isSelected());

                                String nombreCliente = customerInfo[0];
                                String localSeleccionado = customerInfo[1];
                                boolean incluirPrecioSugerido = Boolean.parseBoolean(customerInfo[2]);


                                String logoPath = getLogoPath(); // Ruta del logo

                                // Seleccionar ubicación y nombre del archivo
                                String savePath = FacturaUtils.seleccionarUbicacionYNombreArchivo2(primaryStage, numeroFactura);

                                // Generar la factura en PDF
                                FacturaUtils.generarPresupuestoPDF2(nombreCliente, localSeleccionado, productosEnCaja, logoPath, savePath, incluirPrecioSugerido);


                                Platform.runLater(() -> {

                                        //notificacion pdf guardado
                                        NotificationManager.showNotification("El archivo PDF se guardó correctamente en:\n" + savePath, primaryStage);

                                        new Thread(() -> generarExcelPresupuesto2(primaryStage, nombreCliente, localSeleccionado, productosEnCaja)).start();

                                });


                        }
                });

        }


        private void guardarNumeroFactura() {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(numeroFacturaFilePath))) {
                        writer.write(String.valueOf(numeroFactura));
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        private void cargarNumeroFactura() {
                File file = new File(numeroFacturaFilePath);
                if (!file.exists()) {
                        try {
                                file.createNewFile();
                                numeroFactura = 1000; // Valor inicial
                                guardarNumeroFactura(); // Guardar el valor inicial en el archivo
                        } catch (IOException e) {
                                e.printStackTrace();
                        }
                } else {
                        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                                String numeroFacturaStr = reader.readLine();
                                if (numeroFacturaStr != null) {
                                        numeroFactura = Integer.parseInt(numeroFacturaStr);
                                } else {
                                        numeroFactura = 1000; // Valor por defecto si el archivo está vacío
                                        guardarNumeroFactura(); // Guardar el valor por defecto en el archivo
                                }
                        } catch (IOException e) {
                                numeroFactura = 1000; // Valor por defecto si no se puede leer el archivo
                                e.printStackTrace();
                        }
                }
        }


        public String getLogoPath() {
                String clientLogoPath = "app-resources/logo.png";
                File logoFile = new File(clientLogoPath);
                if (logoFile.exists()) {
                        return clientLogoPath;
                } else {
                        // Usa el logo predeterminado
                        return getClass().getResource("/images/default_logo.png").toExternalForm();
                }
        }

        //generar presupuesto1
        private void generarExcelPresupuesto1(Stage stage, String nombreCliente, String local, List<ProductoEnCaja> productos) {
            AtomicReference<String> rutaGuardada = new AtomicReference<>(prefs.get(PREFS_KEY, null));
            AtomicReference<File> archivoExcel = new AtomicReference<>();

            // Validar la ruta guardada y la existencia del archivo
            boolean rutaValida = rutaGuardada.get() != null && !rutaGuardada.get().isEmpty();
            boolean archivoExiste = false;

            if (rutaValida) {
                    archivoExcel.set(new File(rutaGuardada + File.separator + "presupuestos.xlsx"));
                    archivoExiste = archivoExcel.get().exists();
            }

            // Si la ruta no es válida o el archivo no existe, solicitar una nueva ruta
            if (!rutaValida || !archivoExiste) {
                    System.out.println("Ruta no válida o archivo no encontrado. Solicitando nueva carpeta...");

                    // Ejecutar en el hilo de la GUI
                    Platform.runLater(() -> {
                            DirectoryChooser directoryChooser = new DirectoryChooser();
                            directoryChooser.setTitle("Selecciona la carpeta para guardar el archivo Excel");
                            File selectedDirectory = directoryChooser.showDialog(stage);

                            if (selectedDirectory != null) {
                                    rutaGuardada.set(selectedDirectory.getAbsolutePath());
                                    prefs.put(PREFS_KEY, rutaGuardada.get()); // Guardamos la nueva ruta para futuras ejecuciones
                                    archivoExcel.set(new File(rutaGuardada + File.separator + "presupuestos.xlsx"));

                                    // Continuar con la generación del archivo Excel
                                    procesarArchivoExcel(archivoExcel.get(), nombreCliente, local, productos, stage);
                            } else {
                                    System.out.println("No se seleccionó ninguna carpeta.");
                            }
                    });
            } else {
                    // Si la ruta es válida y el archivo existe, proceder directamente
                    procesarArchivoExcel(archivoExcel.get(), nombreCliente, local, productos, stage);
            }

        }

        private void procesarArchivoExcel(File archivoExcel, String nombreCliente, String local, List<ProductoEnCaja> productos, Stage stage) {
                try {
                        boolean resetearDatos = false;
                        LocalDate fechaActual = LocalDate.now();

                        if (archivoExcel.exists()) {
                                LocalDate fechaUltimaModificacion = LocalDate.ofInstant(
                                        Instant.ofEpochMilli(archivoExcel.lastModified()), ZoneId.systemDefault());

                                if (ChronoUnit.DAYS.between(fechaUltimaModificacion, fechaActual) > 7) {
                                        resetearDatos = true;
                                }
                        } else {
                                resetearDatos = true; // Si el archivo no existe, se crea un nuevo archivo
                        }

                        Workbook workbook;
                        Sheet sheet;

                        // Si se debe resetear los datos o el archivo no existe, crear un nuevo archivo
                        if (resetearDatos) {
                                System.out.println("Reseteando y creando un nuevo archivo...");
                                workbook = new XSSFWorkbook();
                                sheet = workbook.createSheet("Presupuestos");

                                // Crear fila de encabezados con estilo
                                CellStyle headerStyle = createCellStyleHeader(workbook, IndexedColors.LAVENDER);
                                Row headerRow = sheet.createRow(0);
                                String[] headers = {"Fecha", "N° Presupuesto", "Cliente", "Local", "Código", "Descripción","Sabor", "Cantidad","", "Monto", "Ganancia"};
                                for (int i = 0; i < headers.length; i++) {
                                        Cell cell = headerRow.createCell(i);
                                        cell.setCellValue(headers[i]);
                                        cell.setCellStyle(headerStyle);
                                }
                        } else {
                                workbook = new XSSFWorkbook(Files.newInputStream(Paths.get(archivoExcel.getAbsolutePath())));
                                sheet = workbook.getSheetAt(0);
                        }

                        int lastRow = sheet.getLastRowNum();
                        //Definir la primera fila que contiene la nueva fecha
                        int startRow = lastRow + 1;
                        String fechaActualExcel = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                        // Crear un estilo para moneda sin decimales/Monto-Ganancia
                        CellStyle montoStyle = createCellStyleMoney(workbook,IndexedColors.LIGHT_YELLOW);
                        CellStyle gananciaStyle = createCellStyleMoney(workbook,IndexedColors.BRIGHT_GREEN);

                        // Estilo de celda centrado para las otras celdas
                        CellStyle centeredStyle = workbook.createCellStyle();
                        centeredStyle.setAlignment(HorizontalAlignment.CENTER);
                        centeredStyle.setVerticalAlignment(VerticalAlignment.CENTER);

                        //estilo con color fondo para fecha, nro presupuesto, cliente
                        CellStyle centeredStyleColor = createCellStyle(workbook,IndexedColors.YELLOW);

                        for (ProductoEnCaja producto : productos) {
                                Row row = sheet.createRow(++lastRow);
                                // fecha
                                Cell cellFecha = row.createCell(0);
                                cellFecha .setCellValue(fechaActualExcel);
                                cellFecha .setCellStyle(centeredStyleColor);

                                // n° presupuesto
                                Cell cellPresupuesto = row.createCell(1);
                                cellPresupuesto.setCellValue(numeroFactura);
                                cellPresupuesto.setCellStyle(centeredStyleColor);

                                // Cliente
                                Cell cellCliente = row.createCell(2);
                                cellCliente.setCellValue(nombreCliente);
                                cellCliente.setCellStyle(centeredStyleColor);

                                // Local
                                Cell cellLocal = row.createCell(3);
                                cellLocal.setCellValue(local);
                                cellLocal.setCellStyle(centeredStyle);

                                // Código
                                Cell cellCodigo = row.createCell(4);
                                cellCodigo.setCellValue(producto.getProducto().getCodigo());
                                cellCodigo.setCellStyle(centeredStyle);

                                // Descripción
                                Cell cellDescripcion = row.createCell(5);
                                cellDescripcion.setCellValue(producto.getProducto().getDescripcion());
                                cellDescripcion.setCellStyle(centeredStyle);

                                // Sabor
                                Cell cellSabor = row.createCell(6);
                                cellSabor.setCellValue(producto.getProducto().getSabor());
                                cellSabor.setCellStyle(centeredStyle);

                                // Cantidad
                                Cell cellCantidad = row.createCell(7);
                                cellCantidad.setCellValue(producto.getCantidad());
                                cellCantidad.setCellStyle(centeredStyle);

                                // vacio
                                Cell cellVacio = row.createCell(8);
                                cellVacio.setCellValue(" ");
                                cellVacio.setCellStyle(centeredStyle);

                                // Monto
                                Cell montoCell = row.createCell(9);
                                montoCell.setCellValue(Math.round(getTotal1(productos)));
                                montoCell.setCellStyle(montoStyle);

                                // Ganancia
                                Cell gananciaCell = row.createCell(10);
                                gananciaCell.setCellValue(Math.round(getGanancia1(productos)));
                                gananciaCell.setCellStyle(gananciaStyle);
                        }

                        // Combinar celdas de las columnas Fecha, N° Presupuesto, Cliente, Local, Monto, y Ganancia
                        if (startRow != lastRow) {
                                for (int i = 0; i <= 3; i++) { // Combina Fecha, N° Presupuesto, Cliente, Local
                                        sheet.addMergedRegion(new CellRangeAddress(startRow, lastRow, i, i));
                                }
                                for (int i = 9; i <= 10; i++) { // Combina Monto y Ganancia
                                        sheet.addMergedRegion(new CellRangeAddress(startRow, lastRow, i, i));

                                }
                        }

                        // Aplicar borde inferior a la última fila del presupuesto
                        Row lastRowData = sheet.getRow(lastRow);
                        for (int i = 0; i <= 10; i++) {
                                Cell cell = lastRowData.getCell(i);
                                if (cell != null) {
                                        CellStyle newStyle = workbook.createCellStyle();
                                        newStyle.cloneStyleFrom(cell.getCellStyle());
                                        newStyle.setBorderBottom(BorderStyle.THICK);
                                        newStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
                                        cell.setCellStyle(newStyle);
                                }
                        }

                        AtomicBoolean guardado = new AtomicBoolean(false);

                        while (!guardado.get()) {
                                try (FileOutputStream fos = new FileOutputStream(archivoExcel)) {
                                        workbook.write(fos);
                                        System.out.println("Archivo Excel guardado en: " + archivoExcel.getAbsolutePath());
                                        guardado.set(true); // Se ha guardado correctamente, salimos del bucle
                                } catch (IOException e) {
                                        Platform.runLater(() -> {
                                                Alert alert = new Alert(Alert.AlertType.WARNING, "El archivo está abierto en otro programa. Ciérralo y pulsa 'Aceptar' para guardar los cambios.", ButtonType.OK);
                                                alert.showAndWait().ifPresent(buttonType -> {
                                                        if (buttonType == ButtonType.OK) {
                                                                guardado.set(false);
                                                        }
                                                });
                                        });

                                        try {
                                                Thread.sleep(10000); // Espera 10 segundos antes de reintentar
                                        } catch (InterruptedException ie) {
                                                ie.printStackTrace();
                                        }
                                }
                        }

                        workbook.close();

                        // Mostrar mensaje de confirmación al usuario
                        Platform.runLater(() -> NotificationManager.showNotification("El archivo Excel se guardó correctamente en: " + archivoExcel.getAbsolutePath(), stage));

                } catch (IOException e) {
                        e.printStackTrace();
                }
        }


        // Método para calcular el total de todos los productos en caja
        public  static double getTotal1(List<ProductoEnCaja> productos){
            return productos.stream()
                        .mapToDouble(p -> p.getProducto().getPrecioMayorista1() * p.getCantidad())
                        .sum();
        }

        public static double getGanancia1(List<ProductoEnCaja> productos){
            return productos.stream()
                        .mapToDouble(p -> (p.getProducto().getPrecioMayorista1() - p.getProducto().getPrecioBase()) * p.getCantidad())
                        .sum();
        }
        //---------------------------presupuesto2

        private void generarExcelPresupuesto2(Stage stage, String nombreCliente, String local, List<ProductoEnCaja> productos) {
                AtomicReference<String> rutaGuardada = new AtomicReference<>(prefs.get(PREFS_KEY, null));
                AtomicReference<File> archivoExcel = new AtomicReference<>();

                // Validar la ruta guardada y la existencia del archivo
                boolean rutaValida = rutaGuardada.get() != null && !rutaGuardada.get().isEmpty();
                boolean archivoExiste = false;

                if (rutaValida) {
                        archivoExcel.set(new File(rutaGuardada + File.separator + "presupuestos.xlsx"));
                        archivoExiste = archivoExcel.get().exists();
                }

                // Si la ruta no es válida o el archivo no existe, solicitar una nueva ruta
                if (!rutaValida || !archivoExiste) {
                        System.out.println("Ruta no válida o archivo no encontrado. Solicitando nueva carpeta...");

                        // Ejecutar en el hilo de la GUI
                        Platform.runLater(() -> {
                                DirectoryChooser directoryChooser = new DirectoryChooser();
                                directoryChooser.setTitle("Selecciona la carpeta para guardar el archivo Excel");
                                File selectedDirectory = directoryChooser.showDialog(stage);

                                if (selectedDirectory != null) {
                                        rutaGuardada.set(selectedDirectory.getAbsolutePath());
                                        prefs.put(PREFS_KEY, rutaGuardada.get()); // Guardamos la nueva ruta para futuras ejecuciones
                                        archivoExcel.set(new File(rutaGuardada + File.separator + "presupuestos.xlsx"));

                                        // Continuar con la generación del archivo Excel
                                        procesarArchivoExcel2(archivoExcel.get(), nombreCliente, local, productos, stage);
                                } else {
                                        System.out.println("No se seleccionó ninguna carpeta.");
                                }
                        });
                } else {
                        // Si la ruta es válida y el archivo existe, proceder directamente
                        procesarArchivoExcel2(archivoExcel.get(), nombreCliente, local, productos, stage);
                }

        }

        private void procesarArchivoExcel2(File archivoExcel, String nombreCliente, String local, List<ProductoEnCaja> productos, Stage stage) {
                try {
                        boolean resetearDatos = false;
                        LocalDate fechaActual = LocalDate.now();

                        if (archivoExcel.exists()) {
                                LocalDate fechaUltimaModificacion = LocalDate.ofInstant(
                                        Instant.ofEpochMilli(archivoExcel.lastModified()), ZoneId.systemDefault());

                                if (ChronoUnit.DAYS.between(fechaUltimaModificacion, fechaActual) > 7) {
                                        resetearDatos = true;
                                }
                        } else {
                                resetearDatos = true; // Si el archivo no existe, se crea un nuevo archivo
                        }

                        Workbook workbook;
                        Sheet sheet;

                        // Si se debe resetear los datos o el archivo no existe, crear un nuevo archivo
                        if (resetearDatos) {
                                System.out.println("Reseteando y creando un nuevo archivo...");
                                workbook = new XSSFWorkbook();
                                sheet = workbook.createSheet("Presupuestos");

                                // Crear fila de encabezados con estilo
                                CellStyle headerStyle = createCellStyleHeader(workbook, IndexedColors.LAVENDER);
                                Row headerRow = sheet.createRow(0);
                                String[] headers = {"Fecha", "N° Presupuesto", "Cliente", "Local", "Código", "Descripción","Sabor", "Cantidad","", "Monto", "Ganancia"};
                                for (int i = 0; i < headers.length; i++) {
                                        Cell cell = headerRow.createCell(i);
                                        cell.setCellValue(headers[i]);
                                        cell.setCellStyle(headerStyle);
                                }
                        } else {
                                workbook = new XSSFWorkbook(Files.newInputStream(Paths.get(archivoExcel.getAbsolutePath())));
                                sheet = workbook.getSheetAt(0);
                        }

                        int lastRow = sheet.getLastRowNum();
                        //Definir la primera fila que contiene la nueva fecha
                        int startRow = lastRow + 1;
                        String fechaActualExcel = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                        // Crear un estilo para moneda sin decimales/Monto-Ganancia
                        CellStyle montoStyle = createCellStyleMoney(workbook,IndexedColors.LIGHT_YELLOW);
                        CellStyle gananciaStyle = createCellStyleMoney(workbook,IndexedColors.BRIGHT_GREEN);

                        // Estilo de celda centrado para las otras celdas
                        CellStyle centeredStyle = workbook.createCellStyle();
                        centeredStyle.setAlignment(HorizontalAlignment.CENTER);
                        centeredStyle.setVerticalAlignment(VerticalAlignment.CENTER);

                        //estilo con color fondo para fecha, nro presupuesto, cliente
                        CellStyle centeredStyleColor = createCellStyle(workbook,IndexedColors.YELLOW);

                        for (ProductoEnCaja producto : productos) {
                                Row row = sheet.createRow(++lastRow);
                                // fecha
                                Cell cellFecha = row.createCell(0);
                                cellFecha .setCellValue(fechaActualExcel);
                                cellFecha .setCellStyle(centeredStyleColor);

                                // n° presupuesto
                                Cell cellPresupuesto = row.createCell(1);
                                cellPresupuesto.setCellValue(numeroFactura);
                                cellPresupuesto.setCellStyle(centeredStyleColor);

                                // Cliente
                                Cell cellCliente = row.createCell(2);
                                cellCliente.setCellValue(nombreCliente);
                                cellCliente.setCellStyle(centeredStyleColor);

                                // Local
                                Cell cellLocal = row.createCell(3);
                                cellLocal.setCellValue(local);
                                cellLocal.setCellStyle(centeredStyle);

                                // Código
                                Cell cellCodigo = row.createCell(4);
                                cellCodigo.setCellValue(producto.getProducto().getCodigo());
                                cellCodigo.setCellStyle(centeredStyle);

                                // Descripción
                                Cell cellDescripcion = row.createCell(5);
                                cellDescripcion.setCellValue(producto.getProducto().getDescripcion());
                                cellDescripcion.setCellStyle(centeredStyle);

                                // Descripción
                                Cell cellSabor = row.createCell(6);
                                cellSabor.setCellValue(producto.getProducto().getSabor());
                                cellSabor.setCellStyle(centeredStyle);

                                // Cantidad
                                Cell cellCantidad = row.createCell(7);
                                cellCantidad.setCellValue(producto.getCantidad());
                                cellCantidad.setCellStyle(centeredStyle);

                                // vacio
                                Cell cellVacio = row.createCell(8);
                                cellVacio.setCellValue(" ");
                                cellVacio.setCellStyle(centeredStyle);

                                // Monto
                                Cell montoCell = row.createCell(9);
                                montoCell.setCellValue(Math.round(getTotal2(productos)));
                                montoCell.setCellStyle(montoStyle);

                                // Ganancia
                                Cell gananciaCell = row.createCell(10);
                                gananciaCell.setCellValue(Math.round(getGanancia2(productos)));
                                gananciaCell.setCellStyle(gananciaStyle);
                        }

                        // Combinar celdas de las columnas Fecha, N° Presupuesto, Cliente, Local, Monto, y Ganancia
                        if (startRow != lastRow) {
                                for (int i = 0; i <= 3; i++) { // Combina Fecha, N° Presupuesto, Cliente, Local
                                        sheet.addMergedRegion(new CellRangeAddress(startRow, lastRow, i, i));
                                }
                                for (int i = 9; i <= 10; i++) { // Combina Monto y Ganancia
                                        sheet.addMergedRegion(new CellRangeAddress(startRow, lastRow, i, i));

                                }
                        }

                        // Aplicar borde inferior a la última fila del presupuesto
                        Row lastRowData = sheet.getRow(lastRow);
                        for (int i = 0; i <= 10; i++) {
                                Cell cell = lastRowData.getCell(i);
                                if (cell != null) {
                                        CellStyle newStyle = workbook.createCellStyle();
                                        newStyle.cloneStyleFrom(cell.getCellStyle());
                                        newStyle.setBorderBottom(BorderStyle.THICK);
                                        newStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
                                        cell.setCellStyle(newStyle);
                                }
                        }

                        AtomicBoolean guardado = new AtomicBoolean(false);

                        while (!guardado.get()) {
                                try (FileOutputStream fos = new FileOutputStream(archivoExcel)) {
                                        workbook.write(fos);
                                        System.out.println("Archivo Excel guardado en: " + archivoExcel.getAbsolutePath());
                                        guardado.set(true); // Se ha guardado correctamente, salimos del bucle
                                } catch (IOException e) {
                                        Platform.runLater(() -> {
                                                Alert alert = new Alert(Alert.AlertType.WARNING, "El archivo está abierto en otro programa. Ciérralo y pulsa 'Aceptar' para guardar los cambios.", ButtonType.OK);
                                                alert.showAndWait().ifPresent(buttonType -> {
                                                        if (buttonType == ButtonType.OK) {
                                                                guardado.set(false);
                                                        }
                                                });
                                        });

                                        try {
                                                Thread.sleep(10000); // Espera 10 segundos antes de reintentar
                                        } catch (InterruptedException ie) {
                                                ie.printStackTrace();
                                        }
                                }
                        }

                        workbook.close();

                        // Mostrar mensaje de confirmación al usuario
                        Platform.runLater(() -> NotificationManager.showNotification("El archivo Excel se guardó correctamente en: " + archivoExcel.getAbsolutePath(), stage));

                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        //metodo de estilo y color columna

        private static CellStyle createCellStyleHeader(Workbook workbook, IndexedColors color) {
                CellStyle style = workbook.createCellStyle();

                // Alineación horizontal y vertical
                style.setAlignment(HorizontalAlignment.CENTER);
                style.setVerticalAlignment(VerticalAlignment.CENTER);

                // Color de fondo
                style.setFillForegroundColor(color.getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                // Fuente en negrita
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);

                // Bordes
                style.setBorderTop(BorderStyle.THIN);
                style.setBorderBottom(BorderStyle.THIN);
                style.setBorderLeft(BorderStyle.THIN);
                style.setBorderRight(BorderStyle.THIN);

                return style;
        }
        private static CellStyle createCellStyle(Workbook workbook, IndexedColors color) {
                CellStyle style = workbook.createCellStyle();
                style.setAlignment(HorizontalAlignment.CENTER);
                style.setVerticalAlignment(VerticalAlignment.CENTER);
                style.setFillForegroundColor(color.getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                return style;
        }

        private static CellStyle createCellStyleMoney(Workbook workbook, IndexedColors color) {
                CellStyle style = workbook.createCellStyle();
                DataFormat format = workbook.createDataFormat();
                style.setAlignment(HorizontalAlignment.CENTER);
                style.setVerticalAlignment(VerticalAlignment.CENTER);
                style.setDataFormat(format.getFormat("$#,##0")); // Formato de moneda sin decimales
                style.setFillForegroundColor(color.getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                return style;
        }


                // Método para calcular el total de todos los productos en caja
        public  static double getTotal2(List<ProductoEnCaja> productos){
                double total = productos.stream()
                        .mapToDouble(p -> p.getProducto().getPrecioFinal2() * p.getCantidad())
                        .sum();
                return total;
        }

        public static double getGanancia2(List<ProductoEnCaja> productos){
                double totalGanancia = productos.stream()
                        .mapToDouble(p -> (p.getProducto().getPrecioFinal2() - p.getProducto().getPrecioBase()) * p.getCantidad())
                        .sum();
                return totalGanancia;
        }

}

