def mensaje():
    import sys
    return "Python %s cargado correctamente desde Chaquopy" % sys.version.split()[0]


def version_python():
    import sys
    return sys.version.split()[0]
