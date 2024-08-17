package org.comercio.gestionProductos.utils;
import lombok.Data;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.comercio.gestionProductos.models.Producto;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Data
public class ExcelReader {
    private double montoMayorista1;
    private double montoMayorista2;
    private double montoMayorista3;



    @SuppressWarnings("DuplicatedCode")
    public void leerMontosMayoristas(String excelFilePath) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(excelFilePath);
        Workbook workbook = new XSSFWorkbook(fileInputStream);
        Sheet sheet = workbook.getSheet("data"); // Acceder a la hoja por su nombre

        if (sheet == null) {
            throw new IllegalArgumentException("El archivo Excel no contiene una hoja llamada 'data'.");
        }

        Map<String, Integer> headerMap = new HashMap<>();
        Row headerRow = sheet.getRow(2); // Cambiado a fila 3


        // Verificar que headerRow no sea null
        if (headerRow == null) {
            throw new IllegalArgumentException("El archivo Excel no contiene datos en la fila de encabezados esperada.");
        }

        for (Cell cell : headerRow) {
            headerMap.put(cell.getStringCellValue(), cell.getColumnIndex());
        }

        if (!headerMap.containsKey("Monto Mayorista1") ||
                !headerMap.containsKey("Monto Mayorista2") ||
                !headerMap.containsKey("Monto Mayorista3")) {
            throw new IllegalArgumentException("El archivo Excel no contiene las columnas esperadas para los montos mayoristas.");
        }


        for (Cell cell : headerRow) {
            if (cell.getColumnIndex() != 4 && cell.getColumnIndex() != 5 && cell.getColumnIndex() != 6) { // Ignorar columnas E, F y G
                headerMap.put(cell.getStringCellValue(), cell.getColumnIndex());
            }
        }

        // Verificar que la fila que contiene los datos no sea null
        Row dataRow = sheet.getRow(3); // Asumiendo que la fila con los montos está en la fila 4 (índice 3)
        if (dataRow == null) {
            throw new IllegalArgumentException("El archivo Excel no contiene datos en la fila esperada para los montos mayoristas.");
        }

        // Verificar que las celdas no sean null antes de obtener el valor
        Cell cellMonto1 = dataRow.getCell(headerMap.get("Monto Mayorista1"));
        Cell cellMonto2 = dataRow.getCell(headerMap.get("Monto Mayorista2"));
        Cell cellMonto3 = dataRow.getCell(headerMap.get("Monto Mayorista3"));

        montoMayorista1 = (cellMonto1 != null) ? cellMonto1.getNumericCellValue() : 0.0;
        montoMayorista2 = (cellMonto2 != null) ? cellMonto2.getNumericCellValue() : 0.0;
        montoMayorista3 = (cellMonto3 != null) ? cellMonto3.getNumericCellValue() : 0.0;

        workbook.close();
        fileInputStream.close();
    }

    public List<Producto> leerProductos(String excelFilePath) throws IOException {
        List<Producto> listaProductos = new ArrayList<>();
        FileInputStream fileInputStream = new FileInputStream(excelFilePath);
        Workbook workbook = new XSSFWorkbook(fileInputStream);
        Sheet sheet = workbook.getSheet("data");

        if (sheet == null) {
            throw new IllegalArgumentException("El archivo Excel no contiene una hoja llamada 'data'.");
        }

        Map<String, Integer> headerMap = new HashMap<>();
        Row headerRow = sheet.getRow(2);

        for (Cell cell : headerRow) {
            if (cell.getColumnIndex() != 4 && cell.getColumnIndex() != 5 && cell.getColumnIndex() != 6) { // Ignorar columnas E, F y G
                headerMap.put(cell.getStringCellValue(), cell.getColumnIndex());
            }
        }


        for (String expectedHeader : new String[]{
                "Código", "Marca", "Sabor", "Descripción",
                "Precio Base", "Porcentaje Precio Sugerido", "Porcentaje Precio Mayorista1",
                "Porcentaje Precio Mayorista2", "Porcentaje Precio Mayorista3", "Descuento"}) {

            if (!headerMap.containsKey(expectedHeader)) {
                throw new IllegalArgumentException("El archivo Excel no contiene la columna esperada: " + expectedHeader);
            }
        }

        // Iterar sobre las filas comenzando desde la fila 4 (índice 3)
        for (int rowIndex = 3; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);

            if (row == null || isRowEmpty(row)) {
                continue; // Saltar filas vacías
            }

            Producto producto = new Producto();
            producto.setCodigo(getStringCellValue(row,headerMap.get("Código")));
            producto.setMarca(getStringCellValue(row,headerMap.get("Marca")));
            producto.setSabor(getStringCellValue(row,headerMap.get("Sabor")));
            producto.setDescripcion(getStringCellValue(row,headerMap.get("Descripción")));
            producto.setPrecioBase(getNumericCellValue(row,headerMap.get("Precio Base")));
            producto.setPorcentajePrecioSugerido(getNumericCellValue(row,headerMap.get("Porcentaje Precio Sugerido")));
            producto.setPorcentajePrecioMayorista1(getNumericCellValue(row,headerMap.get("Porcentaje Precio Mayorista1")));
            producto.setPorcentajePrecioMayorista2(getNumericCellValue(row,headerMap.get("Porcentaje Precio Mayorista2")));
            producto.setPorcentajePrecioMayorista3(getNumericCellValue(row,headerMap.get("Porcentaje Precio Mayorista3")));
            producto.setPorcentajeDescuento2(getNumericCellValue(row,headerMap.get("Descuento")));

            producto.calcularPreciosMasSugerido();

            listaProductos.add(producto);
        }

        workbook.close();
        fileInputStream.close();
        return listaProductos;
    }

    public List<String> leerLocales(String excelFilePath) throws IOException {
        List<String> listaLocales = new ArrayList<>();
        FileInputStream fileInputStream = new FileInputStream(excelFilePath);
        Workbook workbook = new XSSFWorkbook(fileInputStream);
        Sheet sheet = workbook.getSheet("data");
        String encabezado = "Locales";

        if (sheet == null) {
            throw new IllegalArgumentException("El archivo Excel no contiene una hoja llamada 'data'.");
        }

        // Encontrar el índice de la columna con el encabezado deseado
        Row headerRow = sheet.getRow(2); // La fila 3 es el índice 2 (0-based index)

        int columnIndex = -1;
        for (Cell cell : headerRow) {
            if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().equalsIgnoreCase(encabezado)) {
                columnIndex = cell.getColumnIndex();
                break;
            }
        }

        if (columnIndex == -1) {
            throw new IllegalArgumentException("El encabezado '" + encabezado + "' no se encontró en la fila 3.");
        }

        // Leer los datos de la columna correspondiente
        for (Row row : sheet) {
            if (row.getRowNum() < 3) {
                continue; // Saltar las primeras 3 filas (encabezados y filas no relevantes)
            }

            Cell cellLocal = row.getCell(columnIndex);
            if (cellLocal != null && cellLocal.getCellType() == CellType.STRING) {
                listaLocales.add(cellLocal.getStringCellValue());
            }
        }

        workbook.close();
        fileInputStream.close();
        return listaLocales;
    }


    private String getStringCellValue(Row row, Integer columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if(cell != null && !cell.getStringCellValue().isEmpty() && !cell.getStringCellValue().isBlank()){
            return cell.getStringCellValue();
        }
        else{
            return "   -";
        }
    }

    private double getNumericCellValue(Row row, Integer columnIndex) {
        Cell cell = row.getCell(columnIndex);
        return (cell != null) ? cell.getNumericCellValue() : 0.0;
    }

    private boolean isRowEmpty(Row row) {
        // Recorrer todas las celdas de la fila
        for (Cell cell : row) {
            // Si encontramos una celda no vacía, la fila no está vacía
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        // Si llegamos aquí, todas las celdas están vacías o son null
        return true;
    }
}
