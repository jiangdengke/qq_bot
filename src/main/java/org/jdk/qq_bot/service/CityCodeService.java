package org.jdk.qq_bot.service;

import lombok.RequiredArgsConstructor;
import org.jdk.qq_bot.dto.CodesResponse;
import org.jdk.qq_bot.repository.CityCodeRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CityCodeService {
  private final CityCodeRepository repo;

  /** 根据中文名（模糊）返回 adCode、cityCode；找不到返回 null */
  public CodesResponse getCodesByName(String nameZh) {
    if (nameZh == null || nameZh.isBlank()) return null;
    var r = repo.findTopByNameLike(nameZh.trim());
    return (r == null) ? null : new CodesResponse(r.value1(), r.value2());
  }
}
