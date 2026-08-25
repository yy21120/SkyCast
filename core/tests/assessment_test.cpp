#include "skycast/core/assessment.h"

#include <chrono>
#include <cstdlib>
#include <iostream>
#include <string_view>

namespace {

void expect(const bool condition, const std::string_view message) {
    if (!condition) {
        std::cerr << "FAILED: " << message << '\n';
        std::exit(EXIT_FAILURE);
    }
}
}  // namespace

int main() {
    using namespace std::chrono_literals;
    using skycast::core::AssessmentResult;
    using skycast::core::Confidence;
    using skycast::core::DataKind;
    using skycast::core::Freshness;

    const auto radar_policy = skycast::core::default_policy(DataKind::radar);
    expect(
        skycast::core::classify_freshness(6min, radar_policy) == Freshness::fresh,
        "radar data at the fresh boundary should be fresh");
    expect(
        skycast::core::classify_freshness(7min, radar_policy) == Freshness::degraded,
        "radar data after the fresh boundary should be degraded");
    expect(
        skycast::core::classify_freshness(13min, radar_policy) == Freshness::expired,
        "radar data beyond the expiry boundary should be expired");
    expect(
        skycast::core::classify_freshness(-1min, radar_policy) == Freshness::expired,
        "future-dated observations should not be treated as fresh");

    const AssessmentResult valid{
        .score = 82,
        .probability = 0.76,
        .confidence = Confidence::medium,
        .model_version = "sunset-rules-v0",
        .provenance = {"weather-provider:test", "satellite:test"},
    };
    expect(skycast::core::validate(valid).empty(), "valid assessment should pass");

    const AssessmentResult invalid{
        .score = 101,
        .probability = -0.1,
        .confidence = Confidence::low,
        .model_version = "",
        .provenance = {},
    };
    expect(
        skycast::core::validate(invalid).size() == 4,
        "invalid assessment should report every contract violation");

    std::cout << "All skycast_core tests passed.\n";
    return EXIT_SUCCESS;
}
