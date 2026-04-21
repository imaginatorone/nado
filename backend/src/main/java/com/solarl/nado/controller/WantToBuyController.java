package com.solarl.nado.controller;

import com.solarl.nado.dto.request.WantToBuyRequest;
import com.solarl.nado.dto.response.WantToBuyResponse;
import com.solarl.nado.dto.response.WantedMatchResponse;
import com.solarl.nado.service.WantToBuyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/want-to-buy")
@RequiredArgsConstructor
public class WantToBuyController {

    private final WantToBuyService service;

    @PostMapping
    public ResponseEntity<WantToBuyResponse> create(@Valid @RequestBody WantToBuyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<WantToBuyResponse>> getMyRequests() {
        return ResponseEntity.ok(service.getMyRequests());
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // матчи для конкретного запроса
    @GetMapping("/{id}/matches")
    public ResponseEntity<List<WantedMatchResponse>> getMatches(@PathVariable Long id) {
        return ResponseEntity.ok(service.getMatches(id));
    }

    // пометить матчи как просмотренные
    @PostMapping("/{id}/matches/seen")
    public ResponseEntity<Void> markSeen(@PathVariable Long id) {
        service.markMatchesSeen(id);
        return ResponseEntity.noContent().build();
    }

    // общее кол-во непросмотренных матчей
    @GetMapping("/unseen-count")
    public ResponseEntity<Map<String, Long>> getUnseenCount() {
        return ResponseEntity.ok(Map.of("count", service.getUnseenCount()));
    }
}
