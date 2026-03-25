package com.example.demo5.batch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.example.demo5.entity.BorrowRecord;
import com.example.demo5.repository.BorrowRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BorrowRecordCsvExportTask {

    private static final Logger log = LoggerFactory.getLogger(BorrowRecordCsvExportTask.class);
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BorrowRepository borrowRepository;

    @Value("${borrow.export.output-dir:exports/borrows}")
    private String outputDir;

    public BorrowRecordCsvExportTask(BorrowRepository borrowRepository) {
        this.borrowRepository = borrowRepository;
    }

    @Scheduled(
            cron = "${borrow.export.cron:0 5 0 * * *}",
            zone = "${borrow.export.zone:Asia/Tokyo}")
    @Transactional(readOnly = true)
    public void exportYesterdayBorrowRecords() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime start = yesterday.atStartOfDay();
        LocalDateTime end = yesterday.plusDays(1).atStartOfDay();

        List<BorrowRecord> records = borrowRepository
                .findByBorrowedAtGreaterThanEqualAndBorrowedAtLessThanOrderByBorrowedAtAsc(start, end);

        String fileName = "borrow_records_" + yesterday.format(FILE_DATE_FORMAT) + ".csv";
        Path outputPath = Paths.get(outputDir).resolve(fileName);

        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, buildCsv(records), StandardCharsets.UTF_8);
            log.info("Exported {} borrow records to {}", records.size(), outputPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to export borrow records CSV to {}", outputPath.toAbsolutePath(), e);
        }
    }

    private String buildCsv(List<BorrowRecord> records) {
        StringBuilder csv = new StringBuilder();
        csv.append("id,userId,username,bookId,bookTitle,borrowedAt,dueDate,returnedAt,status\n");

        for (BorrowRecord record : records) {
            csv.append(record.getId()).append(',')
                    .append(escape(record.getUser() == null ? null : String.valueOf(record.getUser().getId()))).append(',')
                    .append(escape(record.getUser() == null ? null : record.getUser().getUsername())).append(',')
                    .append(escape(record.getBook() == null ? null : String.valueOf(record.getBook().getId()))).append(',')
                    .append(escape(record.getBook() == null ? null : record.getBook().getTitle())).append(',')
                    .append(escape(formatDateTime(record.getBorrowedAt()))).append(',')
                    .append(escape(formatDateTime(record.getDueDate()))).append(',')
                    .append(escape(formatDateTime(record.getReturnedAt()))).append(',')
                    .append(escape(record.getStatus() == null ? null : record.getStatus().name()))
                    .append('\n');
        }

        return csv.toString();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FORMAT);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (!needsQuotes) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
