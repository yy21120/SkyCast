#pragma once

#include <chrono>
#include <string>
#include <string_view>
#include <vector>

namespace skycast::core {

enum class DataKind {
    radar,
    satellite,
    forecast,
};

enum class Freshness {
    fresh,
    degraded,
    expired,
};

enum class Confidence {
    low,
    medium,
    high,
};

struct FreshnessPolicy {
    std::chrono::minutes fresh_until;
    std::chrono::minutes expires_after;
};

struct AssessmentResult {
    int score{};
    double probability{};
    Confidence confidence{Confidence::low};
    std::string model_version;
    std::vector<std::string> provenance;
};

[[nodiscard]] FreshnessPolicy default_policy(DataKind kind);

[[nodiscard]] Freshness classify_freshness(
    std::chrono::minutes age,
    FreshnessPolicy policy);

[[nodiscard]] std::vector<std::string> validate(
    const AssessmentResult& assessment);

[[nodiscard]] std::string_view to_string(Freshness freshness);

}  // namespace skycast::core
