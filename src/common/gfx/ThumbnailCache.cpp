#include "ThumbnailCache.h"

#include "SDL_image.h"

#include <system_error>

namespace thumbnail_cache {

SdlSurfacePtr load(const std::filesystem::path& path)
{
    std::error_code error;
    if (!std::filesystem::is_regular_file(path, error))
        return {};

    SdlSurfacePtr image(IMG_Load(path.string().c_str()));
    if (!image || image->w != 160 || image->h != 120)
        return {};
    return image;
}

bool save(SDL_Surface* surface, const std::filesystem::path& path)
{
    std::error_code error;
    if (!path.parent_path().empty())
        std::filesystem::create_directories(path.parent_path(), error);
    if (error) {
        SDL_LogWarn(SDL_LOG_CATEGORY_APPLICATION, "Cannot create thumbnail cache %s: %s",
            path.parent_path().string().c_str(), error.message().c_str());
        return false;
    }

    if (IMG_SavePNG(surface, path.string().c_str()) != 0) {
        SDL_LogWarn(SDL_LOG_CATEGORY_APPLICATION, "Cannot save thumbnail %s: %s",
            path.string().c_str(), IMG_GetError());
        return false;
    }
    return true;
}

} // namespace thumbnail_cache
