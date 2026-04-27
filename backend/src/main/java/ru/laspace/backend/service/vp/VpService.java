package ru.laspace.backend.service.vp;

import ru.laspace.backend.dto.vp.VpCreateRequest;

public interface VpService {
    Long saveVp(VpCreateRequest request);
}
