#pragma once

#include "util/SdlHelpers.h"

#include <filesystem>

namespace thumbnail_cache {

// A cache miss (including an unreadable or damaged PNG) is not a game error.
SdlSurfacePtr load(const std::filesystem::path& path);

// Best effort only: callers must keep their generated surface if persistence fails.
bool save(SDL_Surface* surface, const std::filesystem::path& path);

} // namespace thumbnail_cache
