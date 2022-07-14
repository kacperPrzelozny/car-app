import com.fasterxml.uuid.Generators;
import com.google.gson.Gson;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import static spark.Spark.*;

public class App {
    static ArrayList<Car> cars = new ArrayList<>();
    static int getHerokuPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567;
    }
    public static void main(String[] args) {
        port(getHerokuPort());
        staticFiles.location("/public");
        Spark.post("/add", (req, res) -> addCar(req,res));
        Spark.post("/dane", (req, res) -> sendData(req, res));
        Spark.post("/delete", (req, res) -> deleteCar(req, res));
        Spark.post("/edit", (req, res) -> editCar(req, res));
        Spark.post("/generate", (req, res) -> generateCars(req, res));
        Spark.post("/pdf", (req, res) -> generatePDF(req, res));
        Spark.get("/downloadPDF", (req, res) -> downloadPDF(req, res));
        Spark.post("generateAllCars", (req, res) -> generateAllCars(req, res) );
        Spark.get("/downloadAllCarsPDF", (req, res) -> downloadAllCarsPDF(req, res));
        Spark.post("generateCarsByYear", (req, res) -> generateCarsByYearPDF(req, res));
        Spark.get("/downloadCarsByYearPDF", (req, res) -> downloadCarsByYearPDF(req, res));
        Spark.post("generateCarsByPrice", (req, res) -> generateCarsByPricePDF(req, res));
        Spark.get("/downloadCarsByPricePDF", (req, res) -> downloadCarsByPricePDF(req, res));
    }
    public static String sendData(Request req, Response res){
        Gson gson = new Gson();
        res.type("application/json");
        return gson.toJson(cars, ArrayList.class);
    }
    public static String addCar(Request req, Response res){
        UUID uuid = Generators.randomBasedGenerator().generate();
        int id = cars.size() + 1;
        Gson gson = new Gson();
        Car car = gson.fromJson(req.body(), Car.class);
        car.setId(id);
        car.setUuid(String.valueOf(uuid));
        cars.add(car);
        res.type("application/json");
        return gson.toJson(car, Car.class);
    }
    public static String deleteCar(Request req, Response res){
        Gson gson = new Gson();
        Delete del = gson.fromJson(req.body(), Delete.class);
        String uuid = del.getUuid();
        for(Car car : cars){
            if(car.getUuid().equals(uuid)){
                cars.remove(car);
            }
        }
        return gson.toJson(cars, ArrayList.class);
    }
    public static String editCar(Request req, Response res){
        Gson gson = new Gson();
        Edit edit = gson.fromJson(req.body(), Edit.class);
        String uuid = edit.getUuid();
        for(Car car : cars){
            if(car.getUuid().equals(uuid)) {
                car.setModel(edit.getModel());
                car.setYear(edit.getYear());
            }
        }
        return gson.toJson(cars, ArrayList.class);
    }
    public static String generateCars(Request req, Response res){
        String[] models = {"Fiat", "Renault", "Opel", "Skoda"};
        int[] years = {2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010};
        String[] colors = {"red", "black", "gray", "darkblue"};
        int[] vat = {0,7,22};
        Boolean[] flag = {true, false};
        String[] descriptions = {"kierowca", "pasazer", "kanapa", "boczne"};
        for(int i=1; i<11; i++){
            ArrayList<Airbags> airbags = new ArrayList<>();
            for(int j=0; j<4; j++){
                int random = (int) Math.floor(Math.random()*2);
                Airbags airbag = new Airbags(descriptions[j], flag[random]);
                airbags.add(airbag);
            }
            int year = years[(int) Math.floor(Math.random()*10)];
            int buyYear = year + 1;
            int buyMonth = (int) Math.floor(Math.random()*(12-1+1)+1);
            int buyDay = (int) Math.floor(Math.random()*(28-1+1)+1);
            int price = (int) Math.floor(Math.random()*(100000-10000+1)+10000);
            int vatValue = vat[(int) Math.floor(Math.random()*3)];
            String randomDate = buyDay + "/" + buyMonth + "/" + buyYear;
            String model = models[(int) Math.floor(Math.random()*4)];
            String color = colors[(int) Math.floor(Math.random()*4)];
            Car car = new Car(model, year, color, airbags, randomDate, vatValue, price);
            UUID uuid = Generators.randomBasedGenerator().generate();
            int id = cars.size() + 1;
            car.setId(id);
            car.setUuid(String.valueOf(uuid));
            cars.add(car);
        }
        Gson gson = new Gson();
        return gson.toJson(cars, ArrayList.class);
    }
    public static String generatePDF(Request req, Response res) throws IOException, DocumentException {
        Gson gson = new Gson();

        Delete ob = gson.fromJson(req.body(), Delete.class);
        String uuid = ob.getUuid();
        String model = "";
        int year = 2000;
        String color = "black";
        ArrayList<Airbags> airbags = null;
        Boolean[] values = new Boolean[4];
        String[] keys = {"kierowca", "pasażer", "boczne", "tylna kanapa"};
        for(Car car : cars)
            if (car.getUuid().equals(uuid)) {
                model = car.getModel();
                year = car.getYear();
                color = car.getColor();
                airbags = car.getAirbags();
                car.setPdf(true);
            }
        for(int i=0; i<4; i++){
            Airbags airbag = airbags.get(i);
            values[i] = airbag.getValue();
        }
        HashMap<String, BaseColor> map = new HashMap<>() {{
            put("red", BaseColor.RED);
            put("darkblue", BaseColor.BLUE);
            put("black", BaseColor.BLACK);
            put("gray", BaseColor.GRAY);
        }};

        Document document = new Document();
        String path =  "pdf/" + uuid + ".pdf";
        File invoiceFile = new File(path);
        if (!invoiceFile.getParentFile().exists())
            invoiceFile.getParentFile().mkdirs();
        if (!invoiceFile.exists()) {
            try {
                invoiceFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        PdfWriter.getInstance(document, new FileOutputStream(path));

        document.open();
        Font font = FontFactory.getFont(FontFactory.COURIER_BOLD, 18, BaseColor.BLACK);
        Font font2 = FontFactory.getFont(FontFactory.COURIER, 20, BaseColor.BLACK);
        Font font3 = FontFactory.getFont(FontFactory.COURIER, 16, map.get(color));
        Font font4 = FontFactory.getFont(FontFactory.COURIER, 16, BaseColor.BLACK);

        Paragraph title = new Paragraph("FAKTURA dla " + uuid, font);

        Paragraph modelParagraph = new Paragraph("Model: " + model, font2);
        Paragraph colorParagraph = new Paragraph("kolor: " + color, font3);
        Paragraph yearParagraph = new Paragraph("Rok: " + year, font4);

        document.add(title);
        document.add(modelParagraph);
        document.add(colorParagraph);
        document.add(yearParagraph);

        for(int i = 0; i < 4; i++){
            Paragraph airbagParagraph = new Paragraph("Poduszka: " + keys[i] + " - " + values[i], font4);
            document.add(airbagParagraph);
        }

        Image img = Image.getInstance("./target/classes/public/"+model.toLowerCase(Locale.ROOT) + ".jpg");
        document.add(img);


        document.close();
        return gson.toJson(cars, ArrayList.class);
    }
    public static String downloadPDF(Request req, Response res) throws IOException {
        res.type("application/octet-stream"); //
        String uuid = req.queryParams("uuid");
        res.header("Content-Disposition", "attachment; filename="+uuid+".pdf"); // nagłówek
        OutputStream outputStream = res.raw().getOutputStream();
        outputStream.write(Files.readAllBytes(Path.of("pdf/" + uuid + ".pdf"))); // response pliku do przeglądarki
        return "";
    }
    public static String generateAllCars(Request req, Response res) throws FileNotFoundException, DocumentException {
        Gson gson = new Gson();

        Document document = new Document();
        long timestamp = System.currentTimeMillis();
        String path =  "pdf/invoice_all_cars_" + timestamp + ".pdf";
        File invoiceFile = new File(path);
        if (!invoiceFile.getParentFile().exists())
            invoiceFile.getParentFile().mkdirs();
        if (!invoiceFile.exists()) {
            try {
                invoiceFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();
        Font font = FontFactory.getFont(FontFactory.COURIER_BOLD, 20, BaseColor.BLACK);
        Font font2 = FontFactory.getFont(FontFactory.COURIER, 16, BaseColor.BLACK);
        Font font3 = FontFactory.getFont(FontFactory.COURIER_BOLD, 18, BaseColor.RED);
        Font font4 = FontFactory.getFont(FontFactory.COURIER, 14, BaseColor.BLACK);
        Paragraph title = new Paragraph("FAKTURA VAT: " + java.time.LocalDate.now(), font);

        Paragraph buyer = new Paragraph("Nabywca: firma kupująca auta", font2);
        Paragraph seller = new Paragraph("Sprzedawca: firma sprzedająca auta", font2);
        Paragraph p = new Paragraph("Faktura za wszystkie auta: ", font3);

        document.add(title);
        document.add(buyer);
        document.add(seller);
        document.add(p);

        PdfPTable table = new PdfPTable(4);
        table.setSpacingBefore(15);
        table.setSpacingAfter(15);

        PdfPCell lp1 = new PdfPCell(new Phrase("lp", font4));
        table.addCell(lp1);
        PdfPCell price1 = new PdfPCell(new Phrase("cena netto", font4));
        table.addCell(price1);
        PdfPCell vat1 = new PdfPCell(new Phrase("vat", font4));
        table.addCell(vat1);
        PdfPCell bruttoPrice1 = new PdfPCell(new Phrase("cena brutto", font4));
        table.addCell(bruttoPrice1);
        int counter = 1;

        float suma = 0;
        for(Car car : cars){
            PdfPCell lp = new PdfPCell(new Phrase(String.valueOf(counter), font4));
            table.addCell(lp);
            PdfPCell price = new PdfPCell(new Phrase(String.valueOf(car.getPrice()), font4));
            table.addCell(price);
            PdfPCell vat = new PdfPCell(new Phrase(String.valueOf(car.getVat())+"%", font4));
            table.addCell(vat);
            int priceValue = car.getPrice();
            int v = car.getVat();
            float podatek = (priceValue*v)/100;
            PdfPCell bruttoPrice = new PdfPCell(new Phrase(String.valueOf(podatek+priceValue), font4));
            table.addCell(bruttoPrice);
            suma+=podatek+priceValue;
            counter++;
        }
        document.add(table);
        Paragraph sumaP = new Paragraph("DO ZAPŁATY: " + suma, font2);
        document.add(sumaP);

        document.close();

        ArrayList<String> toSend = new ArrayList<>();
        toSend.add(String.valueOf(timestamp));
        return gson.toJson(toSend, ArrayList.class);
    }
    public static String downloadAllCarsPDF(Request req, Response res) throws IOException {
        res.type("application/octet-stream"); //
        String timestamp = req.queryParams("timestamp");
        res.header("Content-Disposition", "attachment; filename="+timestamp+".pdf"); // nagłówek
        OutputStream outputStream = res.raw().getOutputStream();
        outputStream.write(Files.readAllBytes(Path.of("pdf/invoice_all_cars_" + timestamp + ".pdf"))); // response pliku do przeglądarki
        return "";
    }
    public static String generateCarsByYearPDF(Request req, Response res) throws DocumentException, FileNotFoundException {
        Gson gson = new Gson();
        CarByYears ob = gson.fromJson(req.body(), CarByYears.class);
        int carYear = ob.year;
        Document document = new Document();
        long timestamp = System.currentTimeMillis();
        String path =  "pdf/invoice_all_cars_by_year_" + timestamp + ".pdf";
        File invoiceFile = new File(path);
        if (!invoiceFile.getParentFile().exists())
            invoiceFile.getParentFile().mkdirs();
        if (!invoiceFile.exists()) {
            try {
                invoiceFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();
        Font font = FontFactory.getFont(FontFactory.COURIER_BOLD, 20, BaseColor.BLACK);
        Font font2 = FontFactory.getFont(FontFactory.COURIER, 16, BaseColor.BLACK);
        Font font3 = FontFactory.getFont(FontFactory.COURIER_BOLD, 18, BaseColor.RED);
        Font font4 = FontFactory.getFont(FontFactory.COURIER, 14, BaseColor.BLACK);
        Paragraph title = new Paragraph("FAKTURA VAT: " + java.time.LocalDate.now(), font);

        Paragraph buyer = new Paragraph("Nabywca: firma kupująca auta", font2);
        Paragraph seller = new Paragraph("Sprzedawca: firma sprzedająca auta", font2);
        Paragraph p = new Paragraph("Faktura za wszystkie auta z roku: " + carYear, font3);

        document.add(title);
        document.add(buyer);
        document.add(seller);
        document.add(p);

        int counter = 1;
        PdfPTable table = new PdfPTable(5);
        table.setSpacingBefore(15);
        table.setSpacingAfter(15);
        PdfPCell lp1 = new PdfPCell(new Phrase("lp", font4));
        table.addCell(lp1);
        PdfPCell yearP1 = new PdfPCell(new Phrase("rocznik", font4));
        table.addCell(yearP1);
        PdfPCell price1 = new PdfPCell(new Phrase("cena netto", font4));
        table.addCell(price1);
        PdfPCell vat1 = new PdfPCell(new Phrase("vat", font4));
        table.addCell(vat1);
        PdfPCell bruttoPrice1 = new PdfPCell(new Phrase("cena brutto", font4));
        table.addCell(bruttoPrice1);
        float suma = 0;
        for(Car car : cars){
            if(car.getYear()==carYear) {
                PdfPCell lp = new PdfPCell(new Phrase(String.valueOf(counter), font4));
                table.addCell(lp);
                PdfPCell yearP = new PdfPCell(new Phrase(String.valueOf(car.getYear()), font4));
                table.addCell(yearP);
                PdfPCell price = new PdfPCell(new Phrase(String.valueOf(car.getPrice()), font4));
                table.addCell(price);
                PdfPCell vat = new PdfPCell(new Phrase(String.valueOf(car.getVat()) + "%", font4));
                table.addCell(vat);
                int priceValue = car.getPrice();
                int v = car.getVat();
                float podatek = (priceValue * v) / 100;
                PdfPCell bruttoPrice = new PdfPCell(new Phrase(String.valueOf(podatek + priceValue), font4));
                table.addCell(bruttoPrice);
                suma += podatek + priceValue;
                counter++;
            }
        }
        document.add(table);
        Paragraph sumaP = new Paragraph("DO ZAPŁATY: " + suma, font2);
        document.add(sumaP);

        document.close();

        ArrayList<String> toSend = new ArrayList<>();
        toSend.add(String.valueOf(timestamp));
        return gson.toJson(toSend, ArrayList.class);
    }
    public static String downloadCarsByYearPDF(Request req, Response res) throws IOException {
        res.type("application/octet-stream"); //
        String timestamp = req.queryParams("timestamp");
        res.header("Content-Disposition", "attachment; filename="+timestamp+".pdf"); // nagłówek
        OutputStream outputStream = res.raw().getOutputStream();
        outputStream.write(Files.readAllBytes(Path.of("pdf/invoice_all_cars_by_year_" + timestamp + ".pdf"))); // response pliku do przeglądarki
        return "";
    }
    public static String generateCarsByPricePDF(Request req, Response res) throws DocumentException, FileNotFoundException {
        Gson gson = new Gson();
        CarByPrice ob = gson.fromJson(req.body(), CarByPrice.class);
        int min = ob.min;
        int max = ob.max;

        Document document = new Document();
        long timestamp = System.currentTimeMillis();
        String path =  "pdf/invoice_all_cars_by_price_" + timestamp + ".pdf";
        File invoiceFile = new File(path);
        if (!invoiceFile.getParentFile().exists())
            invoiceFile.getParentFile().mkdirs();
        if (!invoiceFile.exists()) {
            try {
                invoiceFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        PdfWriter.getInstance(document, new FileOutputStream(path));
        document.open();
        Font font = FontFactory.getFont(FontFactory.COURIER_BOLD, 20, BaseColor.BLACK);
        Font font2 = FontFactory.getFont(FontFactory.COURIER, 16, BaseColor.BLACK);
        Font font3 = FontFactory.getFont(FontFactory.COURIER_BOLD, 18, BaseColor.RED);
        Font font4 = FontFactory.getFont(FontFactory.COURIER, 14, BaseColor.BLACK);
        Paragraph title = new Paragraph("FAKTURA VAT: " + java.time.LocalDate.now(), font);

        Paragraph buyer = new Paragraph("Nabywca: firma kupująca auta", font2);
        Paragraph seller = new Paragraph("Sprzedawca: firma sprzedająca auta", font2);
        Paragraph p = new Paragraph("Faktura za wszystkie auta z przedziału cenowego od: "+min+" do:" + max, font3);

        document.add(title);
        document.add(buyer);
        document.add(seller);
        document.add(p);

        int counter = 1;
        PdfPTable table = new PdfPTable(5);
        table.setSpacingBefore(15);
        table.setSpacingAfter(15);
        PdfPCell lp1 = new PdfPCell(new Phrase("lp", font4));
        table.addCell(lp1);
        PdfPCell yearP1 = new PdfPCell(new Phrase("rocznik", font4));
        table.addCell(yearP1);
        PdfPCell price1 = new PdfPCell(new Phrase("cena netto", font4));
        table.addCell(price1);
        PdfPCell vat1 = new PdfPCell(new Phrase("vat", font4));
        table.addCell(vat1);
        PdfPCell bruttoPrice1 = new PdfPCell(new Phrase("cena brutto", font4));
        table.addCell(bruttoPrice1);
        float suma = 0;
        for(Car car : cars){
            if(car.getPrice()>=min&&car.getPrice()<=max) {
                PdfPCell lp = new PdfPCell(new Phrase(String.valueOf(counter), font4));
                table.addCell(lp);
                PdfPCell yearP = new PdfPCell(new Phrase(String.valueOf(car.getYear()), font4));
                table.addCell(yearP);
                PdfPCell price = new PdfPCell(new Phrase(String.valueOf(car.getPrice()), font4));
                table.addCell(price);
                PdfPCell vat = new PdfPCell(new Phrase(String.valueOf(car.getVat()) + "%", font4));
                table.addCell(vat);
                int priceValue = car.getPrice();
                int v = car.getVat();
                float podatek = (priceValue * v) / 100;
                PdfPCell bruttoPrice = new PdfPCell(new Phrase(String.valueOf(podatek + priceValue), font4));
                table.addCell(bruttoPrice);
                suma += podatek + priceValue;
                counter++;
            }
        }
        document.add(table);
        Paragraph sumaP = new Paragraph("DO ZAPŁATY: " + suma, font2);
        document.add(sumaP);

        document.close();

        ArrayList<String> toSend = new ArrayList<>();
        toSend.add(String.valueOf(timestamp));
        return gson.toJson(toSend, ArrayList.class);
    }
    public static String downloadCarsByPricePDF(Request req, Response res) throws IOException {
        res.type("application/octet-stream"); //
        String timestamp = req.queryParams("timestamp");
        res.header("Content-Disposition", "attachment; filename="+timestamp+".pdf"); // nagłówek
        OutputStream outputStream = res.raw().getOutputStream();
        outputStream.write(Files.readAllBytes(Path.of("pdf/invoice_all_cars_by_price_" + timestamp + ".pdf"))); // response pliku do przeglądarki
        return "";
    }
}
