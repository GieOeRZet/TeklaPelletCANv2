cmake_minimum_required(VERSION 3.16)
set(EXTRA_COMPONENT_DIRS components)
include($ENV{IDF_PATH}/tools/cmake/project.cmake)
# gesty styl jednolinijkowcow: ostrzezenie misleading-indentation nie jest bledem
idf_build_set_property(COMPILE_OPTIONS "-Wno-misleading-indentation" APPEND)
project(tekla_pellet)
