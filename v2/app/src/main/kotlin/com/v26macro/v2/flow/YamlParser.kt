package com.v26macro.v2.flow

import java.io.InputStream

/**
 * YAML → Flow 역직렬화. snakeyaml-engine 으로 Map<String,Any> 받고 Step 으로 변환.
 *
 * Phase 3 에서 채움. 옵션 형태 (`tap_text: "X"` vs `tap_text: { text: X, region: ... }`)
 * 둘 다 파싱.
 */
class YamlParser {
    fun parse(@Suppress("UNUSED_PARAMETER") stream: InputStream): Flow {
        TODO("Phase 3: snakeyaml-engine 으로 파싱 후 Step sealed class 매핑 + 검증 에러 메시지")
    }
}
