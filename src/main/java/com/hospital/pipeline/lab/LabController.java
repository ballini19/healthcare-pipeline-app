package com.hospital.pipeline.lab;

import com.hospital.pipeline.domain.LabResult;
import com.hospital.pipeline.domain.Severity;
import com.hospital.pipeline.repo.LabResultRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/lab")
public class LabController {

    private final LabService service;
    private final LabRunLog runLog;
    private final AlertService alerts;
    private final LabResultRepository results;

    public LabController(LabService service, LabRunLog runLog, AlertService alerts, LabResultRepository results) {
        this.service = service;
        this.runLog = runLog;
        this.alerts = alerts;
        this.results = results;
    }

    /** Runs the batch on an uploaded CSV, or on a freshly generated analyzer feed when none is sent. */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> run(@RequestParam(required = false) MultipartFile file) throws IOException {
        Path path;
        if (file != null && !file.isEmpty()) {
            path = Files.createTempFile("lab-upload-", ".csv");
            file.transferTo(path);
        } else {
            path = service.freshFeed();
        }
        if (!service.startAsync(path)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "A batch run is already in progress"));
        }
        return ResponseEntity.accepted().body(Map.of("message", "Batch started", "file", path.getFileName().toString()));
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("running", runLog.running());
        out.put("last", runLog.last());
        out.put("skipped", runLog.skipped());
        return out;
    }

    @GetMapping("/results")
    public List<LabResult> results(@RequestParam(required = false) Severity severity,
                                   @RequestParam(defaultValue = "40") int limit) {
        if (severity != null) {
            return results.findBySeverity(severity).stream().limit(Math.min(limit, 200)).toList();
        }
        return results.findAllByOrderByIdDesc(PageRequest.of(0, Math.min(limit, 200)));
    }

    @GetMapping("/alerts")
    public Map<String, Object> alerts() {
        return Map.of("total", alerts.total(), "latest", alerts.latest().stream().limit(25).toList());
    }
}
