#include "skycast/core/assessment.h"

namespace skycast::core {

FreshnessPolicy default_policy(const DataKind kind) {
    using namespace std::chrono_literals;

    switch (kind) {
        case DataKind::radar:
            return {.fresh_until = 6min, .expires_after = 12min};
        case DataKind::satellite:
            return {.fresh_until = 15min, .expires_after = 30min};
        case DataKind::forecast:
            return {.fresh_until = 60min, .expires_after = 180min};
    }

    return {.fresh_until = 0min, .expires_after = 0min};
}

Freshness classify_freshness(
    const std::chrono::minutes age,
    const FreshnessPolicy policy) {
    if (age < std::chrono::minutes::zero()) {
        return Freshness::expired;
    }
    if (age <= policy.fresh_until) {
        return Freshness::fresh;
    }
    if (age <= policy.expires_after) {
        return Freshness::degraded;
    }
    return Freshness::expired;
}

std::vector<std::string> validate(const AssessmentResult& assessment) {
    std::vector<std::string> errors;

    if (assessment.score < 0 || assessment.score > 100) {
        errors.emplace_back("score must be in [0, 100]");
    }
    if (assessment.probability < 0.0 || assessment.probability > 1.0) {
        errors.emplace_back("probability must be in [0, 1]");
    }
    if (assessment.model_version.empty()) {
        errors.emplace_back("model_version is required");
    }
    if (assessment.provenance.empty()) {
        errors.emplace_back("at least one provenance entry is required");
    }

    return errors;
}

std::string_view to_string(const Freshness freshness) {
    switch (freshness) {
        case Freshness::fresh:
            return "fresh";
        case Freshness::degraded:
            return "degraded";
        case Freshness::expired:
            return "expired";
    }

    return "expired";
}

}  // namespace skycast::core
