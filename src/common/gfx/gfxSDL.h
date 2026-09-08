#pragma once

#include "SDL.h"


class GraphicsSDL {
public:
    ~GraphicsSDL();
    void shutdown();

    bool init(bool fullscreen);

    void flipScreen();
    void changeFullScreen(bool fullcreen) const;
    void setTitle(const char*) const;
    void showErrorBox(const char*) const;
    void takeScreenshot() const;

private:
    bool recreateRenderer();
    // surface -> texture -> renderer -> window
    SDL_Window* sdl_window = nullptr;
    SDL_Renderer* sdl_renderer = nullptr;
    SDL_Surface* sdl_screen_surface = nullptr;
    SDL_Texture* sdl_screen_texture = nullptr;
};
