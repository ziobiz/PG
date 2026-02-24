package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.entity.AppUser;
import com.pg.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserListService {

    private final UserRepository userRepository;

    public UserListService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public PageResult<Map<String, Object>> search(String searchUserId, String searchUserNm, String searchCompId, int page, int size) {
        Pageable p = PageRequest.of(Math.max(0, page - 1), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.ASC, "username"));
        String uid = (searchUserId != null && !searchUserId.isEmpty()) ? searchUserId : "";
        String nm = (searchUserNm != null && !searchUserNm.isEmpty()) ? searchUserNm : "";
        Page<AppUser> result = userRepository.findByUsernameContainingAndNameContaining(uid, nm, p);
        List<Map<String, Object>> list = result.getContent().stream().map(this::toRow).collect(Collectors.toList());
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(list);
        pr.setPage(result.getNumber() + 1);
        pr.setSize(result.getSize());
        pr.setTotalElements(result.getTotalElements());
        pr.setTotalPages(result.getTotalPages());
        return pr;
    }

    private Map<String, Object> toRow(AppUser u) {
        Map<String, Object> row = new HashMap<>();
        row.put("userId", u.getUsername());
        row.put("userNm", u.getName());
        row.put("compId", "-");
        row.put("roleNm", u.getRole() != null ? u.getRole() : "USER");
        row.put("useYn", u.isEnabled() ? "Y" : "N");
        return row;
    }
}
