---
mode: always
description: "Core development standards, language constraints, and project structure rules."
---

# System Instructions and Development Standards

### 1. Language Constraints

- **Communication Language:** All responses, explanations, logs, and messages must be strictly in Russian.

### 2. Code Cleanliness and Documentation

- **No Comments Policy:** It is strictly forbidden to leave any comments in the code (including single-line, multi-line, JSDoc/Docstrings, and temporary notes). The code must be self-documenting.
- **YAGNI Principle and Conciseness:** Write clean, clear, and minimalistic code. Avoid redundant logic and over-engineering.
- **No Placeholders:** Do not leave unfinished logic, empty functions, or markers such as `TODO`, `FIXME`, or "to be implemented later" in the code. If a piece of functionality is not required at the moment, it must not be written.

### 3. File System and Architecture

- **File Format Restrictions:** Never create files with the `.md` (Markdown) extension unless specifically and explicitly requested by the user.
- **Transparent Architecture:** Strictly adhere to the existing project structure. The location of any created or modified modules must be intuitive, logical, and consistent with the established application architecture patterns.
