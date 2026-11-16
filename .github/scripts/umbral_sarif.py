"""Quality gate para herramientas que solo REPORTAN (CodeQL publica sus hallazgos
en la pestaña Security pero no hace fallar el job).

Lee los archivos SARIF de una carpeta y termina con código 1 si hay algún
hallazgo con security-severity >= umbral (7.0 = alto, 9.0 = crítico, escala CVSS).
Uso: python3 umbral_sarif.py <carpeta-con-sarif> <umbral>
"""
import glob
import json
import os
import sys


def severidad(regla):
    try:
        return float((regla.get("properties") or {}).get("security-severity", 0))
    except (TypeError, ValueError):
        return 0.0


def main(carpeta, umbral):
    bloqueantes = []
    total = 0
    for archivo in glob.glob(os.path.join(carpeta, "*.sarif")):
        with open(archivo, encoding="utf-8") as f:
            sarif = json.load(f)
        for run in sarif.get("runs", []):
            reglas = {r.get("id"): r for r in run.get("tool", {}).get("driver", {}).get("rules", [])}
            for ext in run.get("tool", {}).get("extensions", []) or []:
                for r in ext.get("rules", []) or []:
                    reglas.setdefault(r.get("id"), r)
            for res in run.get("results", []):
                total += 1
                regla = reglas.get(res.get("ruleId"), {})
                sev = severidad(regla)
                if sev >= umbral:
                    ubicacion = (res.get("locations") or [{}])[0].get("physicalLocation", {})
                    ruta = ubicacion.get("artifactLocation", {}).get("uri", "?")
                    linea = ubicacion.get("region", {}).get("startLine", "?")
                    bloqueantes.append(f"  [{sev:.1f}] {res.get('ruleId')} en {ruta}:{linea}: "
                                       f"{res.get('message', {}).get('text', '')[:160]}")
    print(f"Hallazgos de SAST: {total} en total, {len(bloqueantes)} con severidad >= {umbral}")
    if bloqueantes:
        print("Hallazgos que detienen el pipeline (ver docs/seguridad.md para tratarlos):")
        print("\n".join(bloqueantes))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1], float(sys.argv[2])))
