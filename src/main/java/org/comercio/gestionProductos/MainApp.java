package org.comercio.gestionProductos;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.comercio.gestionProductos.controllers.MainController;
import org.comercio.gestionProductos.models.Producto;
import org.comercio.gestionProductos.models.ProductoEnCaja;
import org.comercio.gestionProductos.utils.ExcelReader;

import java.io.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
public class MainApp extends Application {

    private ObservableList<ProductoEnCaja> productosEnCaja = FXCollections.observableArrayList();

    private ObservableList<Producto> productosDisponibles = FXCollections.observableArrayList();

    private TableView<Producto> availableProductsTable = new TableView<>();

    private TableView<ProductoEnCaja> tableView = new TableView<>(productosEnCaja);

    private Label totalLabel = new Label("Total: $0.00");

    private Label subtotalLabel = new Label("Subtotal: $0.00");

    private Label totalGananciaLabel = new Label("Total Ganancia: $0.00");

    private Label totalDiscountLabel = new Label("Descuento Total: $0.00");

    private Label subtotalLabel2 = new Label("Subtotal: $0.00");

    private Label totalDiscountLabel2 = new Label("Descuento Total: $0.00");

    private Label totalLabel2 = new Label("Total: $0.00");

    private Label totalGananciaLabel2 = new Label("Total Ganancia: $0.00");

    private static final DecimalFormat decimalFormat = new DecimalFormat("$#,##0");

    private double montoMayorista1;

    private double montoMayorista2;

    private double montoMayorista3;

    private static final String PREF_KEY_EXCEL_PATH = "excelFilePath";

    // Obtener las preferencias del usuario
    private java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(MainApp.class);

    private String lastFilePath;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private List<String> locales; // Nueva variable para almacenar la lista de locales

    //main
    public static void main(String[] args) {
        launch(args);

    }

    @Override
    public void start(Stage primaryStage) {

        VBox root = new VBox(8); // Espaciado vertical entre elementos
        root.setPadding(new Insets(8)); // Margen alrededor de los bordes

        // Cargar el logo desde la carpeta de la aplicación o usar el logo predeterminado
        Image logo = getLogo();
        primaryStage.getIcons().add(logo);

        // Evento de doble clic para agregar productos

        configurarDobleClic(availableProductsTable);

        // Tabla de productos en caja
        TableColumn<ProductoEnCaja, String> codigoColumn = new TableColumn<>("Cód");
        codigoColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProducto().getCodigo()));
        TableColumn<ProductoEnCaja, String> marcaColumn = new TableColumn<>("Marca");
        marcaColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProducto().getMarca()));
        TableColumn<ProductoEnCaja, String> descripcionColumn = new TableColumn<>("Descripción");
        descripcionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProducto().getDescripcion()));
        TableColumn<ProductoEnCaja, String> saborColumn = new TableColumn<>("Sabor");
        saborColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProducto().getSabor()));
        TableColumn<ProductoEnCaja, Integer> cantidadColumn = new TableColumn<>("Cant");
        cantidadColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getCantidad()).asObject());
        cantidadColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        cantidadColumn.setOnEditCommit(event -> {
            ProductoEnCaja productoEnCaja = event.getRowValue();
            Integer newValue = event.getNewValue();
            // Validar que el valor no sea negativo
            if (newValue == null || newValue < 0) {
                // Mostrar mensaje de error
                mostrarAlerta("Valor inválido","La cantidad no puede ser negativa.");

                // Revertir el cambio
                tableView.refresh();

            } else {
                // Actualizar la cantidad y recalcular el total
                productoEnCaja.setCantidad(newValue);
                recalcularTotal();
                tableView.refresh();
            }
        });
        TableColumn<ProductoEnCaja, Double> precioColumn = crearColumnaPrecio("Precio1", productoEnCaja -> productoEnCaja.getProducto().getPrecioMayorista1());
        TableColumn<ProductoEnCaja, Double> totalPrecio1Column = crearColumnaTotal("SubTotal1",
                productoEnCaja -> productoEnCaja.getProducto().getPrecioMayorista1(), "-fx-background-color: lightgreen;");
        TableColumn<ProductoEnCaja, Double> gananciaPrecio1Column = crearColumnaGanancia1("Subtotal \nGanancia1",
                productoEnCaja -> productoEnCaja.getProducto().getPrecioMayorista1());
        TableColumn<ProductoEnCaja, Double> precioColumn2 = crearColumnaPrecio2("Precio2");
        TableColumn<ProductoEnCaja, Double> totalPrecio2Column = crearColumnaTotal2("SubTotal2",
                productoEnCaja -> productoEnCaja.getProducto().getPrecioMayorista2(), "-fx-background-color: lightblue;");
        TableColumn<ProductoEnCaja, Double> gananciaPrecio2Column = crearColumnaGanancia2("Subtotal \nGanancia2",
                productoEnCaja -> productoEnCaja.getProducto().getPrecioMayorista2());
        TableColumn<ProductoEnCaja, Double> diferenciaColumn = crearColumnaDiferencia("Diferencia",
                (productoEnCaja1, productoEnCaja2) -> {
                    double precioFinal = obtenerPrecioConDescuentoORegular(productoEnCaja2);
                    return productoEnCaja1.getProducto().getPrecioMayorista1() - precioFinal;
                });

        tableView.getColumns().addAll(codigoColumn, marcaColumn, descripcionColumn, saborColumn, cantidadColumn, precioColumn, totalPrecio1Column, gananciaPrecio1Column, precioColumn2, totalPrecio2Column, gananciaPrecio2Column, diferenciaColumn);

        // Hacer que la tabla sea editable
        tableView.setEditable(true);


        // Tabla de productos disponibles
        availableProductsTable.setItems(productosDisponibles);
        TableColumn<Producto, String> availableCodigoColumn = new TableColumn<>("Código");
        availableCodigoColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCodigo()));
        TableColumn<Producto, String> availableMarcaColumn = new TableColumn<>("Marca");
        availableMarcaColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMarca()));
        TableColumn<Producto, String> availableDescripcionColumn = new TableColumn<>("Descripción");
        availableDescripcionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescripcion()));
        TableColumn<Producto, String> availableSaborColumn = new TableColumn<>("Sabor");
        availableSaborColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSabor()));
        TableColumn<Producto, Double> availablePrecio1Column = crearColumnaConColor("Precio 1", Producto::getPrecioMayorista1, "-fx-background-color: lightgreen;");
        TableColumn<Producto, Double> availableGanancia1Column = crearColumna("Ganancia1 \nx producto ", Producto::calcularGananciaProducto1);
        TableColumn<Producto, Double> availablePrecio2Column = crearColumnaConColor("Precio 2", Producto::getPrecioMayorista2, "-fx-background-color: add8e6;");
        TableColumn<Producto, Double> availableGanancia2Column = crearColumna("Ganancia2 \nx producto", Producto::calcularGananciaProducto2);
        TableColumn<Producto, Double> availablePrecioDescuentoColumn = crearColumnaConColor("Precio \nPromo", Producto::getPrecioConDescuento2, "-fx-background-color: #CCB9DD;");
        TableColumn<Producto, Double> availablePrecioCompraColumn = crearColumnaConColor("Precio \nCompra", Producto::getPrecioBase, "-fx-background-color: #f2c3ca;");

        availableProductsTable.getColumns().addAll(availableCodigoColumn, availableMarcaColumn, availableDescripcionColumn, availableSaborColumn, availablePrecio1Column, availableGanancia1Column, availablePrecio2Column,availableGanancia2Column, availablePrecioCompraColumn, availablePrecioDescuentoColumn);


        ///texto de botones
        Button selectFileButton = new Button("Seleccionar Excel");
        Button addButton = new Button("Agregar Producto");
        Button deleteButton = new Button("Eliminar Producto");
        //Button finalizarPedidoButton = new Button("Finalizar Pedido");
        TextField searchField = new TextField();
        searchField.setPromptText("Buscar producto...");
        Button generarPresupuestoButton = new Button("Generar Presupuesto 1");
        Button generarPresupuesto2Button = new Button("Generar Presupuesto 2");
        Button limpiarButton = new Button("Limpiar");


        //validar si ya hay un archivo cargado
        validacionFile();

        // Escuchadores y lógica de botones
        selectFileButton.setOnAction(event -> selectFile(primaryStage));

        // Crear una instancia de MainController
        MainController mainController = new MainController(locales,productosEnCaja);

        addButton.setOnAction(event -> agregarProductoAlPedido());
        deleteButton.setOnAction(event -> eliminarProductoDelPedido());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterProductList(newValue));
        limpiarButton.setOnAction(event -> limpiarCaja());// Botón limpiar

        generarPresupuestoButton.setOnAction(event -> mainController.mostrarPresupuesto(primaryStage));
        generarPresupuesto2Button.setOnAction(event -> mainController.mostrarPresupuesto2(primaryStage));

        //estilos botones tablas
        //buscador
        searchField.setPrefWidth(200); // Establece el ancho preferido

        HBox buttonsTop = new HBox(10, selectFileButton, searchField); // Espaciado horizontal
        buttonsTop.setAlignment(Pos.BASELINE_LEFT);

        VBox buttonsMiddle = new VBox(10, addButton);
        buttonsMiddle.setAlignment(Pos.CENTER_LEFT);

        HBox buttonsCenter = new HBox(10, generarPresupuestoButton, generarPresupuesto2Button, deleteButton, limpiarButton);
        buttonsCenter.setAlignment(Pos.BOTTOM_LEFT);
        buttonsCenter.setPadding(new Insets(5)); // Margen alrededor de los botones

        // Contenedor de cálculos de totales

        VBox totalsContainer = new VBox(5.5, subtotalLabel, totalGananciaLabel, totalDiscountLabel, totalLabel);
        totalsContainer.setAlignment(Pos.CENTER_LEFT);
        totalsContainer.setPadding(new Insets(5)); // Margen alrededor de los totales
        totalsContainer.setStyle("-fx-border-color: #98e898; -fx-border-width: 1.8; -fx-background-color: #f9f9f9; -fx-border-radius: 5; -fx-background-radius: 5;");

        ///totales 2
        VBox totalsContainer2 = new VBox(5.5, subtotalLabel2, totalGananciaLabel2, totalDiscountLabel2, totalLabel2);
        totalsContainer2.setAlignment(Pos.CENTER_LEFT);
        totalsContainer2.setPadding(new Insets(5)); // Margen alrededor de los totales
        totalsContainer2.setStyle("-fx-border-color: #a4caf5; -fx-border-width: 1.8; -fx-background-color: #f9f9f9; -fx-border-radius: 5; -fx-background-radius: 5;");
        //contenedor de totales
        HBox contenedorTotal = new HBox(10, totalsContainer, totalsContainer2);
        contenedorTotal.setAlignment(Pos.CENTER_LEFT);
        contenedorTotal.setPadding(new Insets(12)); // Espacio entre las cajas

        // Estilo de etiquetas de total
        for (Label label : Arrays.asList(subtotalLabel, totalDiscountLabel, totalLabel, totalGananciaLabel, subtotalLabel2, totalDiscountLabel2, totalLabel2, totalGananciaLabel2)) {
            label.setMinHeight(20);
            label.setMinWidth(300);
            label.setStyle("-fx-font-weight: bold;");
        }

        // Aplicar estilos a los botones

        String buttonStyle = "-fx-background-color: %s; -fx-text-fill: black; -fx-border-radius: 5; -fx-background-radius: 5; -fx-border-color: lightgrey; -fx-border-width: 1;";

        selectFileButton.setStyle(String.format(buttonStyle, "#add8e6")); // Light Blue
        addButton.setStyle(String.format(buttonStyle, "#ADC3E6"));
        deleteButton.setStyle(String.format(buttonStyle, "#f08080")); // Light Coral
        generarPresupuestoButton.setStyle(String.format(buttonStyle, "#98e898")); // Light Pink
        generarPresupuesto2Button.setStyle(String.format(buttonStyle, "#a4caf5")); // Plum
        limpiarButton.setStyle(String.format(buttonStyle, "rgba(140,2,37,0.39)")); // Light Cyan

        // Aplicar efectos de hover
        String hoverStyle = "-fx-background-color: %s; -fx-text-fill: white; -fx-border-radius: 5; -fx-background-radius: 5; -fx-border-color: lightgrey; -fx-border-width: 1;";
        selectFileButton.setOnMouseEntered(e -> selectFileButton.setStyle(String.format(hoverStyle, "#9cc9e1"))); // Slightly darker Light Blue
        selectFileButton.setOnMouseExited(e -> selectFileButton.setStyle(String.format(buttonStyle, "#add8e6")));

        addButton.setOnMouseEntered(e -> addButton.setStyle(String.format(hoverStyle, "#9cc9e1"))); // Slightly darker Pale Green
        addButton.setOnMouseExited(e -> addButton.setStyle(String.format(buttonStyle, "#ADC3E6")));

        deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(String.format(hoverStyle, "#de6f6f"))); // Slightly darker Light Coral
        deleteButton.setOnMouseExited(e -> deleteButton.setStyle(String.format(buttonStyle, "#f08080")));

        generarPresupuestoButton.setOnMouseEntered(e -> generarPresupuestoButton.setStyle(String.format(hoverStyle, "#57D757"))); // Slightly darker Light Pink
        generarPresupuestoButton.setOnMouseExited(e -> generarPresupuestoButton.setStyle(String.format(buttonStyle, "#98e898")));

        generarPresupuesto2Button.setOnMouseEntered(e -> generarPresupuesto2Button.setStyle(String.format(hoverStyle, "#5BC1E7"))); // Slightly darker Plum
        generarPresupuesto2Button.setOnMouseExited(e -> generarPresupuesto2Button.setStyle(String.format(buttonStyle, "#a4caf5")));

        limpiarButton.setOnMouseEntered(e -> limpiarButton.setStyle(String.format(hoverStyle, "rgba(140,2,37,0.59)"))); // Slightly darker Light Cyan
        limpiarButton.setOnMouseExited(e -> limpiarButton.setStyle(String.format(buttonStyle, "rgba(140,2,37,0.39)")));


        // Configurar tooltips para los botones
        selectFileButton.setTooltip(new Tooltip("Seleccionar archivo Excel de datos"));
        addButton.setTooltip(new Tooltip("Agregar el producto seleccionado pedido"));
        deleteButton.setTooltip(new Tooltip("Eliminar un producto seleccionado del pedido"));
        generarPresupuestoButton.setTooltip(new Tooltip("Generar Presupuesto en base a monto mayorista 1"));
        generarPresupuesto2Button.setTooltip(new Tooltip("Generar presupuesto en base a monto mayorista 2"));
        limpiarButton.setTooltip(new Tooltip("Limpiar lo productos del pedido"));

        //ajustar columnas a su contenido
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        availableProductsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        //ajustarColumnas(tableView);
        ajustarColumnas(availableProductsTable);

        //centrar tablas CSS
        String cellStyle = "-fx-alignment: CENTER-LEFT;";/* Centrar verticalmente y alinear a la izquierda horizontalmente */
        tableView.getColumns().forEach(column -> column.setStyle(cellStyle));
        availableProductsTable.getColumns().forEach(column -> column.setStyle(cellStyle));

        // Agregar elementos al layout principal
        root.getChildren().addAll(buttonsTop, availableProductsTable, buttonsMiddle, tableView, contenedorTotal, buttonsCenter);

        Scene scene = new Scene(root, 1100, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("linea de caja");
        primaryStage.show();

    }

    //crear columnas
    // Método para crear una columna con color personalizado
    private TableColumn<Producto, Double> crearColumnaConColor(String titulo, Function<Producto, Double> valorFunc, String cellStyle) {
        TableColumn<Producto, Double> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(cellData -> new SimpleDoubleProperty(valorFunc.apply(cellData.getValue())).asObject());
        columna.setCellFactory(column -> new TableCell<Producto, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(decimalFormat.format(item));
                    setStyle(cellStyle);
                }
            }
        });
        return columna;
    }

    private TableColumn<Producto, Double> crearColumna(String titulo, Function<Producto, Double> valorFunc) {
        TableColumn<Producto, Double> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(cellData -> new SimpleDoubleProperty(valorFunc.apply(cellData.getValue())).asObject());
        columna.setCellFactory(column -> new TableCell<Producto, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(decimalFormat.format(item));
                }
            }
        });
        return columna;
    }




    //crear y calcular columnas totales en productos en caja
    private TableColumn<ProductoEnCaja, Double> crearColumnaTotal(String titulo, Function<ProductoEnCaja, Double> precioFunc, String cellStyle) {
        TableColumn<ProductoEnCaja, Double> columnaTotal = new TableColumn<>(titulo);
        columnaTotal.setCellValueFactory(cellData -> {

            double total = precioFunc.apply(cellData.getValue()) * cellData.getValue().getCantidad();

            return new SimpleDoubleProperty(total).asObject();
        });
        columnaTotal.setCellFactory(column -> new TableCell<ProductoEnCaja, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle(""); // Restablecer estilo cuando la celda está vacía
                } else {
                    setText(decimalFormat.format(item));
                    setStyle(cellStyle);
                }
            }
        });
        return columnaTotal;
    }

    private TableColumn<ProductoEnCaja, Double> crearColumnaTotal2(String titulo, Function<ProductoEnCaja, Double> precioFunc, String cellStyle) {
        TableColumn<ProductoEnCaja, Double> columnaTotal = new TableColumn<>(titulo);
        columnaTotal.setCellValueFactory(cellData -> {

            ProductoEnCaja productoEnCaja = cellData.getValue();
            double precioFinal = obtenerPrecioConDescuentoORegular(productoEnCaja); // Usar el método de validación
            double total = precioFinal * productoEnCaja.getCantidad();
            ;

            return new SimpleDoubleProperty(total).asObject();
        });
        columnaTotal.setCellFactory(column -> new TableCell<ProductoEnCaja, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle(""); // Restablecer estilo cuando la celda está vacía
                } else {
                    setText(decimalFormat.format(item));
                    setStyle(cellStyle);
                    // Validar si el precio aplicado es el de descuento y colorear celda
                    ProductoEnCaja productoEnCaja = getTableRow().getItem();
                    aplicarEstiloSiTieneDescuento(this, productoEnCaja);
                }
            }
        });
        return columnaTotal;
    }

    private TableColumn<ProductoEnCaja, Double> crearColumnaGanancia1(String titulo, Function<ProductoEnCaja, Double> precioFunc) {
        TableColumn<ProductoEnCaja, Double> columnaGanancia = new TableColumn<>(titulo);
        columnaGanancia.setCellValueFactory(cellData -> {
            ProductoEnCaja productoEnCaja = cellData.getValue();

            double ganancia = productoEnCaja.calcularTotalGanancia1();

            return new SimpleDoubleProperty(ganancia).asObject();
        });
        columnaGanancia.setCellFactory(column -> new TableCell<ProductoEnCaja, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(decimalFormat.format(item));
                }
            }
        });
        return columnaGanancia;
    }

    private TableColumn<ProductoEnCaja, Double> crearColumnaGanancia2(String titulo, Function<ProductoEnCaja, Double> precioFunc) {
        TableColumn<ProductoEnCaja, Double> columnaGanancia = new TableColumn<>(titulo);
        columnaGanancia.setCellValueFactory(cellData -> {
            ProductoEnCaja productoEnCaja = cellData.getValue();
            //double precioFinal = obtenerPrecioConDescuentoORegular(productoEnCaja);
            double ganancia = productoEnCaja.calcularTotalGanancia2();

            return new SimpleDoubleProperty(ganancia).asObject();
        });
        columnaGanancia.setCellFactory(column -> new TableCell<ProductoEnCaja, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(decimalFormat.format(item));
                    ProductoEnCaja productoEnCaja = getTableRow().getItem();
                    aplicarEstiloSiTieneDescuento(this, productoEnCaja);
                }
            }
        });
        return columnaGanancia;
    }

    private TableColumn<ProductoEnCaja, Double> crearColumnaDiferencia(String titulo, BiFunction<ProductoEnCaja, ProductoEnCaja, Double> diferenciaFunc) {
        TableColumn<ProductoEnCaja, Double> columnaDiferencia = new TableColumn<>(titulo);
        columnaDiferencia.setCellValueFactory(cellData -> {
            double diferencia = diferenciaFunc.apply(cellData.getValue(), cellData.getValue());
            return new SimpleDoubleProperty(diferencia).asObject();
        });
        columnaDiferencia.setCellFactory(column -> new TableCell<ProductoEnCaja, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(decimalFormat.format(item));
                }
            }
        });
        return columnaDiferencia;
    }


    //crear columna precio
    private <T> TableColumn<T, Double> crearColumnaPrecio(String titulo, Function<T, Double> valorFactory) {
        TableColumn<T, Double> column = new TableColumn<>(titulo);
        column.setCellValueFactory(cellData -> new SimpleDoubleProperty(valorFactory.apply(cellData.getValue())).asObject());
        column.setCellFactory(columnP -> new TableCell<T, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(decimalFormat.format(item));
                }
            }
        });
        return column;
    }

    private TableColumn<ProductoEnCaja, Double> crearColumnaPrecio2(String titulo) {
        TableColumn<ProductoEnCaja, Double> column = new TableColumn<>(titulo);
        column.setCellValueFactory(cellData -> {
            ProductoEnCaja productoEnCaja = cellData.getValue();
            double precioFinal = obtenerPrecioConDescuentoORegular(productoEnCaja);
            return new SimpleDoubleProperty(precioFinal).asObject();
        });

        column.setCellFactory(columnP -> new TableCell<ProductoEnCaja, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle(""); // Restablecer estilo cuando la celda está vacía
                } else {
                    setText(decimalFormat.format(item));
                    ProductoEnCaja productoEnCaja = getTableRow().getItem();
                    aplicarEstiloSiTieneDescuento(this, productoEnCaja);
                }
            }
        });

        return column;
    }

    // Ajustar el ancho de las columnas a su contenido
    private void ajustarColumnas(TableView<?> tableView) {
        tableView.getColumns().forEach(column -> {
            // Crear un Text para medir la anchura del encabezado
            Text headerText = new Text(column.getText());
            double maxWidth = headerText.getLayoutBounds().getWidth(); // Ancho inicial basado en el encabezado

            // Iterar sobre las primeras 10 filas de la tabla
            int rowCount = Math.min(tableView.getItems().size(), 1);

            for (int i = 0; i < rowCount; i++) {
                // Obtener el valor de la celda y medir su ancho
                Object cellData = column.getCellData(i);
                if (cellData != null) {
                    Text cellText = new Text(cellData.toString());
                    double width = cellText.getLayoutBounds().getWidth() + 2; // Agregar margen
                    if (width > maxWidth) {
                        maxWidth = width;
                    }
                }
            }

            // Ajustar el ancho de la columna
            column.setPrefWidth(maxWidth);
        });
    }


    private Image getLogo() {
        String logoPath = "app-resources/logo.png";
        File logoFile = new File(logoPath);
        Image logo;
        if (logoFile.exists()) {
            try (InputStream logoStream = new FileInputStream(logoFile)) {
                logo = new Image(logoStream);
            } catch (FileNotFoundException e) {
                logo = new Image(getClass().getResourceAsStream("/images/default_logo2.png"));
            } catch (Exception e) {
                e.printStackTrace();
                logo = new Image(getClass().getResourceAsStream("/images/default_logo2.png"));
            }
        } else {
            logo = new Image(getClass().getResourceAsStream("/images/default_logo2.png"));
        }
        return logo;
    }

    //limpiar productos de pedido
    private void limpiarCaja() {
        productosEnCaja.clear();
        recalcularTotal();
    }


    ///metodo leer excel ok

    /**
     * Abre un diálogo de selección de archivo para elegir un archivo Excel (.xlsx).
     * Si se selecciona un archivo válido, se intenta leer los productos.
     * Los productos leídos se agregan a la lista `productosDisponibles`.
     * guarda las preferencias del usuario en cuanto a ruta
     *
     * @param primaryStage El escenario principal de la aplicación.
     */
    private void selectFile(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos Excel", "*.xlsx"));
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            try {
                loadExcelFile(selectedFile);
                // Guardar la ruta del archivo en las preferencias del usuario
                lastFilePath = selectedFile.getAbsolutePath();
                prefs.put(PREF_KEY_EXCEL_PATH, lastFilePath);
            } catch (IOException e) {
                mostrarAlerta("Error al leer el archivo Excel", e.getMessage());
            }
        }
    }

    private void loadExcelFile(File excelFile) throws IOException {
        ExcelReader excelReader = new ExcelReader();
        List<Producto> products = excelReader.leerProductos(excelFile.getAbsolutePath());
        excelReader.leerMontosMayoristas(excelFile.getAbsolutePath());
        productosDisponibles.setAll(products);

        // Obtener los montos mayoristas
        montoMayorista1 = excelReader.getMontoMayorista1();
        montoMayorista2 = excelReader.getMontoMayorista2();
        montoMayorista3 = excelReader.getMontoMayorista3();

        // Leer locales y almacenarlos en la variable
        locales = excelReader.leerLocales(excelFile.getAbsolutePath());
    }

    private void validacionFile() {
        lastFilePath = prefs.get(PREF_KEY_EXCEL_PATH, null);
        if (lastFilePath != null) {
            try {
                File lastFile = new File(lastFilePath);
                if (lastFile.exists()) {
                    loadExcelFile(lastFile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //metodo filtro buscador ok

    /**
     * Filtra la lista de productos según el texto de búsqueda .
     * Si el texto de búsqueda está vacío o nulo, muestra todos los productos disponibles.
     * De lo contrario, filtra los productos cuyo código, descripción o sabor contengan el texto de búsqueda.
     *
     * @param searchText texto de búsqueda para filtrar los productos.
     */
    private void filterProductList(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            availableProductsTable.setItems(productosDisponibles);
        } else {
            List<Producto> filteredList = productosDisponibles.stream()
                    .filter(producto -> producto.getCodigo().toLowerCase().contains(searchText.toLowerCase()) ||
                            producto.getDescripcion().toLowerCase().contains(searchText.toLowerCase()) ||
                            producto.getSabor().toLowerCase().contains(searchText.toLowerCase()) ||
                            producto.getMarca().toLowerCase().contains(searchText.toLowerCase()))
                    .collect(Collectors.toList());
            availableProductsTable.setItems(FXCollections.observableArrayList(filteredList));
        }
    }

    //metodo agregar producto a pedidos
    private void agregarProductoADesdeDobleClick(Producto producto) {
        boolean productoYaAgregado = false;
        for (ProductoEnCaja productoEnCaja : productosEnCaja) {
            if (productoEnCaja.getProducto().equals(producto)) {
                productoEnCaja.setCantidad(productoEnCaja.getCantidad() + 1);
                productoYaAgregado = true;
                break;
            }
        }
        if (!productoYaAgregado) {
            productosEnCaja.add(new ProductoEnCaja(producto, 1));
        }
        ajustarColumnas(tableView);
        tableView.refresh();
        recalcularTotal();

    }

    // Evento de doble clic para agregar productos
    public void configurarDobleClic(TableView<Producto> availableProductsTable) {
        availableProductsTable.setRowFactory(tv -> {
            TableRow<Producto> row = new TableRow<>();
            row.setOnMouseClicked(event -> manejarClickMouse(event, row));
            return row;
        });
    }

    private void manejarClickMouse(MouseEvent event, TableRow<Producto> row) {
        if (event.getClickCount() == 2 && (!row.isEmpty())) {
            Producto producto = row.getItem();
            scheduler.schedule(() -> Platform.runLater(() -> {

                agregarProductoADesdeDobleClick(producto);

            }), 200, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Agrega un producto al pedido actual.
     * Si el producto ya está en el pedido, incrementa su cantidad en 1.
     * Si el producto no está en el pedido, lo agrega con una cantidad inicial de 1.
     * Después de agregar o actualizar el producto, recalcula el total del pedido.
     */
    private void agregarProductoAlPedido() {
        Producto selectedProduct = availableProductsTable.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            boolean found = false;
            for (ProductoEnCaja p : productosEnCaja) {
                if (p.getProducto().getCodigo().equals(selectedProduct.getCodigo())) {
                    p.setCantidad(p.getCantidad() + 1);
                    found = true;
                    break;
                }
            }
            if (!found) {
                ProductoEnCaja p = new ProductoEnCaja(selectedProduct, 1);
                productosEnCaja.add(p);
            }

            ajustarColumnas(tableView);
            recalcularTotal();
            tableView.refresh();
        } else {
            mostrarAlerta("Producto no seleccionado", "Seleccione un producto de la lista de productos disponibles.");
        }
    }

    /**
     * Eliminar producto de pedidos
     * elimina el produco seleccionado y recalcula el total
     * si la selleccion es nula muestra una alerta
     */
    private void eliminarProductoDelPedido() {
        ProductoEnCaja selectedProduct = tableView.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            productosEnCaja.remove(selectedProduct);
            tableView.refresh();
            recalcularTotal();

        } else {
            mostrarAlerta("Producto no seleccionado", "Seleccione un producto de la lista de productos en caja.");
        }
    }

    private double obtenerPrecioConDescuentoORegular(ProductoEnCaja productoEnCaja) {

        double precioConDescuento = productoEnCaja.getProducto().getPrecioConDescuento2();

        return precioConDescuento > 0 ? precioConDescuento : productoEnCaja.getProducto().getPrecioMayorista2();

    }

    private void aplicarEstiloSiTieneDescuento(TableCell<ProductoEnCaja, Double> cell, ProductoEnCaja productoEnCaja) {
        if (productoEnCaja != null && productoEnCaja.getProducto().getPrecioConDescuento2() > 0) {
            cell.setStyle("-fx-background-color: #CCB9DD;");
        }
    }

    //recalcular total

    /**
     * Recalcula los totales relacionados con el pedido actual y actualiza las etiquetas en la interfaz.
     * Calcula el subtotal (sin descuento), el total de descuentos aplicados y el total final.
     * También verifica y reinicia los descuentos si es necesario.
     */
    private void recalcularTotal() {

        double subtotal = productosEnCaja.stream()
                .mapToDouble(p -> p.getProducto().getPrecioMayorista1() * p.getCantidad())
                .sum();
        subtotalLabel.setText("Subtotal: " + decimalFormat.format(subtotal));

        double total = productosEnCaja.stream()
                .mapToDouble(p -> p.getProducto().getPrecioMayorista1() * p.getCantidad())
                .sum();
        totalLabel.setText("Total: " + decimalFormat.format(total));

        double totalGanancia = productosEnCaja.stream()
                .mapToDouble(p -> (p.getProducto().getPrecioMayorista1() - p.getProducto().getPrecioBase()) * p.getCantidad())
                .sum();
        totalGananciaLabel.setText("Total Ganancia: " + decimalFormat.format(totalGanancia));


        double subtotal2 = productosEnCaja.stream()
                .mapToDouble(p -> p.getProducto().getPrecioMayorista2() * p.getCantidad())
                .sum();
        subtotalLabel2.setText("Subtotal: " + decimalFormat.format(subtotal2));

        double total2 = productosEnCaja.stream()
                .mapToDouble(p -> p.getProducto().getPrecioFinal2() * p.getCantidad())
                .sum();
        totalLabel2.setText("Total: " + decimalFormat.format(total2));

        double totalGanancia2 = productosEnCaja.stream()
                .mapToDouble(p -> (p.getProducto().calcularGananciaProducto2() * p.getCantidad()))
                .sum();
        totalGananciaLabel2.setText("Total Ganancia: " + decimalFormat.format(totalGanancia2));

        double totalDescuento2 = productosEnCaja.stream()
                .mapToDouble(p -> (p.getProducto().getPrecioMayorista2() - p.getProducto().getPrecioFinal2()) * p.getCantidad()).sum();

        totalDiscountLabel2.setText("Total Descuento: " + decimalFormat.format(totalDescuento2));

    }


    //mostrar alertas

    /**
     * Plantilla mpara mostrar alerta
     *
     * @param titulo    titulo del alerta
     * @param contenido texto de alerta
     */
    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

}