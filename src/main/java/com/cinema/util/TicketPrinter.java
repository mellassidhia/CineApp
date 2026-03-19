package com.cinema.util;

import com.cinema.model.Reservation;
import com.cinema.model.Siege;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class TicketPrinter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static String generateTicket(Reservation res) throws Exception {
        String fileName = System.getProperty("user.home") + "/ticket_" + res.getReference() + ".pdf";

        Document doc = new Document(PageSize.A5, 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(fileName));
        doc.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD,  new BaseColor(20, 20, 80));
        Font headerFont= new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,  BaseColor.WHITE);
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,  new BaseColor(60, 60, 60));
        Font valueFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
        Font refFont   = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,  new BaseColor(180, 0, 0));
        Font smallFont = new Font(Font.FontFamily.HELVETICA,  8, Font.ITALIC, BaseColor.GRAY);

        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        PdfPCell bannerCell = new PdfPCell(new Phrase("CINEMA APP", titleFont));
        bannerCell.setBackgroundColor(new BaseColor(20, 20, 80));
        bannerCell.setPadding(16);
        bannerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        bannerCell.setBorder(Rectangle.NO_BORDER);
        banner.addCell(bannerCell);

        PdfPCell subCell = new PdfPCell(new Phrase("BILLET DE RESERVATION", headerFont));
        subCell.setBackgroundColor(new BaseColor(200, 30, 30));
        subCell.setPadding(6);
        subCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        subCell.setBorder(Rectangle.NO_BORDER);
        banner.addCell(subCell);
        doc.add(banner);
        doc.add(Chunk.NEWLINE);

        Paragraph ref = new Paragraph("Reference : " + res.getReference(), refFont);
        ref.setAlignment(Element.ALIGN_CENTER);
        doc.add(ref);
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{35, 65});

        String siegesList = res.getSieges() != null
            ? res.getSieges().stream().map(Siege::getLabel).collect(Collectors.joining(", "))
            : "-";

        addRow(table, "Film",       res.getTitreFilm()   != null ? res.getTitreFilm()  : "-", labelFont, valueFont);
        addRow(table, "Salle",      res.getNumeroSalle() != null ? res.getNumeroSalle(): "-", labelFont, valueFont);
        addRow(table, "Date/Heure", res.getDateSeance()  != null ? res.getDateSeance().format(FMT) : "-", labelFont, valueFont);
        addRow(table, "Client",     res.getNomClient()   != null ? res.getNomClient()  : "-", labelFont, valueFont);
        addRow(table, "Siege(s)",   siegesList, labelFont, valueFont);
        addRow(table, "Prix total", res.getPrixTotal()   != null ? String.format("%.2f DT", res.getPrixTotal()) : "-", labelFont, valueFont);
        addRow(table, "Statut",     res.getStatut()      != null ? res.getStatut()     : "-", labelFont, valueFont);
        doc.add(table);
        doc.add(Chunk.NEWLINE);

        // Barcode using the already-open writer
        try {
            Barcode128 barcode = new Barcode128();
            barcode.setCode(res.getReference());
            barcode.setCodeType(Barcode128.CODE128);
            Image bcImg = barcode.createImageWithBarcode(writer.getDirectContent(), BaseColor.BLACK, BaseColor.WHITE);
            bcImg.setAlignment(Image.ALIGN_CENTER);
            bcImg.scaleToFit(200, 60);
            doc.add(bcImg);
        } catch (Exception ignored) {}

        doc.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph("Merci pour votre visite. Ce billet est personnel et non remboursable.", smallFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
        return fileName;
    }

    private static void addRow(PdfPTable t, String label, String value, Font lf, Font vf) {
        PdfPCell lc = new PdfPCell(new Phrase(label, lf));
        lc.setBackgroundColor(new BaseColor(240, 240, 250));
        lc.setPadding(6);
        lc.setBorderColor(new BaseColor(200, 200, 220));
        t.addCell(lc);
        PdfPCell vc = new PdfPCell(new Phrase(value, vf));
        vc.setPadding(6);
        vc.setBorderColor(new BaseColor(200, 200, 220));
        t.addCell(vc);
    }
}
