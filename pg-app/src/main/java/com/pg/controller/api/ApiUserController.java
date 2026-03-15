package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.PageResult;
import com.pg.service.UserListService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/user", produces = "application/json")
public class ApiUserController {

    private final UserListService userListService;

    public ApiUserController(UserListService userListService) {
        this.userListService = userListService;
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> list(
            @RequestParam(required = false) String searchUserId,
            @RequestParam(required = false) String searchUserNm,
            @RequestParam(required = false) String searchCompId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Map<String, Object>> result = userListService.search(searchUserId, searchUserNm, searchCompId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/menuOrderMng")
    public ResponseEntity<ApiResponse<PageResult<Map<String, Object>>>> menuOrderMng(
            @RequestParam(required = false) String searchMenuId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(new ArrayList<>());
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(0);
        pr.setTotalPages(1);
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }
}
