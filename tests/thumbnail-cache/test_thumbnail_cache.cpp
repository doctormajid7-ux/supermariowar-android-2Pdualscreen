#include "gfx/ThumbnailCache.h"
#include "SDL_image.h"

#include <chrono>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <stdexcept>

namespace fs = std::filesystem;

static void require(bool ok, const char* message) {
    if (!ok) throw std::runtime_error(message);
}

struct TemporaryDirectory {
    fs::path path = fs::temp_directory_path() / ("smw-thumbnails-" +
        std::to_string(std::chrono::steady_clock::now().time_since_epoch().count()));
    TemporaryDirectory() { fs::create_directories(path); }
    ~TemporaryDirectory() { std::error_code ec; fs::remove_all(path, ec); }
};

int main() {
    try {
        require((IMG_Init(IMG_INIT_PNG) & IMG_INIT_PNG) != 0, "PNG initialization failed");
        TemporaryDirectory temp;
        SdlSurfacePtr image(SDL_CreateRGBSurfaceWithFormat(0, 160, 120, 32, SDL_PIXELFORMAT_RGBA32));
        require(bool(image), "Cannot allocate thumbnail");
        SDL_FillRect(image.get(), nullptr, SDL_MapRGBA(image->format, 21, 87, 143, 255));
        fs::path png = temp.path / "maps/cache/0smw.png";

        // Exact first-install failure: neither cache directory nor PNG exists.
        require(!thumbnail_cache::load(png), "Missing cache must be a miss");
        require(thumbnail_cache::save(image.get(), png), "Must create missing parents before saving");
        auto cached = thumbnail_cache::load(png);
        require(cached && cached->w == 160 && cached->h == 120, "Generated PNG must reload");
        Uint8 r, g, b;
        SDL_GetRGB(*static_cast<Uint32*>(cached->pixels), cached->format, &r, &g, &b);
        require(r == 21 && g == 87 && b == 143, "Cached pixels must match generated thumbnail");

        // Partial/corrupted cache files from an earlier failed write must regenerate.
        std::ofstream(png, std::ios::trunc) << "invalid PNG";
        require(!thumbnail_cache::load(png), "Corrupt cache must be a miss, not throw");
        require(thumbnail_cache::save(image.get(), png), "Must replace corrupt cache");
        require(bool(thumbnail_cache::load(png)), "Repaired cache must load");
        std::ofstream(png, std::ios::trunc).close();
        require(!thumbnail_cache::load(png), "Empty cache must be a miss");

        SdlSurfacePtr wrongSize(SDL_CreateRGBSurfaceWithFormat(0, 16, 16, 32, SDL_PIXELFORMAT_RGBA32));
        require(IMG_SavePNG(wrongSize.get(), png.string().c_str()) == 0, "Write wrong-size fixture");
        require(!thumbnail_cache::load(png), "Wrong-sized image must be regenerated");

        // A file in place of a directory reliably simulates an unusable cache,
        // including when tests run as root (where chmod would not prevent writes).
        fs::path blocker = temp.path / "blocked";
        std::ofstream(blocker) << "not a directory";
        require(!thumbnail_cache::save(image.get(), blocker / "cache/0smw.png"), "Cache failure must return false");
        require(image && image->w == 160, "Failed save must retain in-memory thumbnail");
        require(!thumbnail_cache::load(blocker / "cache/0smw.png"), "Unusable cache is a miss");
        require(!thumbnail_cache::save(image.get(), temp.path), "PNG writer failure must return false");

        fs::remove_all(temp.path / "maps");
        require(thumbnail_cache::save(image.get(), png), "Deleted cache must be recreated");
        require(bool(thumbnail_cache::load(png)), "Recreated cache must load");
        IMG_Quit();
        std::cout << "Thumbnail cache regression tests passed\n";
        return 0;
    } catch (const std::exception& error) {
        std::cerr << error.what() << '\n';
        return 1;
    }
}
