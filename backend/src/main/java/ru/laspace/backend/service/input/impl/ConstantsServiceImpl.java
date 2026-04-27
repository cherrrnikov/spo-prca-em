package ru.laspace.backend.service.input.impl;

import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.repository.input.ConstantsRepository;
import ru.laspace.backend.service.input.ConstantsService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConstantsServiceImpl implements ConstantsService {
    private final ConstantsRepository constantsRepository;

    @Override
    public Map<String, Integer> getModeDurations() {
        return constantsRepository.getModeDurationsMap();
    }
}
