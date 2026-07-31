(() => {
  "use strict";

  const VIEW_DEFINITIONS = Object.freeze({
    reviewer: Object.freeze({
      label: "Reviewer",
      title: "Enterprise Lab Reviewer Summary",
      eyebrow: "Read-only posture and boundary review",
      description:
        "Inspect the running application's deterministic reviewer posture, evidence paths, and CI artifact boundary.",
      boundary:
        "Local and CI evidence only. No production certification, live-cloud validation, registry publication, container signing, real-tenant proof, or production SLO proof.",
      endpoint: "/api/enterprise-lab/reviewer-summary",
      filename: "loadbalancerpro-reviewer-summary",
      fallback: Object.freeze({
        source: "static fallback",
        status: "API unavailable",
        nextAction: "Start the local application and refresh this view.",
        safetyBoundaries: Object.freeze([
          "No external network calls",
          "No production certification",
          "No registry publication or container signing"
        ])
      })
    }),
    operator: Object.freeze({
      label: "Operator",
      title: "Operator Evidence Summary",
      eyebrow: "Evidence paths and executable commands",
      description:
        "Locate generated evidence, review packaged proof commands, and keep ignored output outside source control.",
      boundary:
        "Generated evidence remains local or in CI artifacts. This viewer does not scan arbitrary paths, mutate files, or report live GitHub state.",
      endpoint: "/api/enterprise-lab/operator-evidence-summary",
      filename: "loadbalancerpro-operator-evidence",
      fallback: Object.freeze({
        source: "static fallback",
        status: "API unavailable",
        evidenceBoundary: "Generated target output stays ignored and uncommitted.",
        nextAction: "Start the local application and refresh this view."
      })
    }),
    gate: Object.freeze({
      label: "CI Gate",
      title: "CI Evidence Gate Readiness",
      eyebrow: "Prototype contract, not enforcement",
      description:
        "Review candidate local evidence inputs and explicit pass, warning, and blocking semantics before any future CI enforcement.",
      boundary:
        "The gate is not a required check and does not change branch protection, rulesets, secrets, environments, releases, or external systems.",
      endpoint: "/api/enterprise-lab/ci-evidence-gate-summary",
      filename: "loadbalancerpro-ci-evidence-gate",
      fallback: Object.freeze({
        gateName: "CI Evidence Gate",
        decision: "LOCAL_REVIEW_ONLY",
        enforcementStatus: "NOT_ENFORCED",
        nextAction: "Start the local application and refresh this view."
      })
    }),
    timeline: Object.freeze({
      label: "Timeline",
      title: "Evidence Timeline",
      eyebrow: "Ordered proof and boundary stages",
      description:
        "Follow source, verification, package, smoke, CI artifact, reviewer, and operator evidence in a consistent order.",
      boundary:
        "The timeline organizes evidence categories. It does not infer real-time CI state or elevate local proof into production certification.",
      endpoint: "/api/enterprise-lab/evidence-timeline",
      filename: "loadbalancerpro-evidence-timeline",
      fallback: Object.freeze({
        source: "static fallback",
        status: "API unavailable",
        sequence: Object.freeze([
          "source review",
          "verification",
          "package and smoke",
          "CI artifacts",
          "reviewer handoff"
        ])
      })
    }),
    packet: Object.freeze({
      label: "Export Packet",
      title: "Evidence Export Packet",
      eyebrow: "Browser-local reviewer handoff",
      description:
        "Assemble the existing deterministic packet contract, then copy, download, or print it without server-side file creation.",
      boundary:
        "Browser-local handoff only. Do not include secrets, tokens, private keys, credentials, or production targets.",
      endpoint: "/api/enterprise-lab/evidence-export-packet",
      filename: "loadbalancerpro-evidence-packet",
      fallback: Object.freeze({
        source: "static fallback",
        status: "API unavailable",
        exportBoundary: "The server does not create, upload, or share files.",
        nextAction: "Start the local application and refresh this view."
      })
    })
  });

  const LEGACY_PATHS = Object.freeze({
    "/enterprise-lab-reviewer.html": "reviewer",
    "/operator-evidence-dashboard.html": "operator",
    "/ci-evidence-gate.html": "gate",
    "/evidence-timeline.html": "timeline",
    "/evidence-export-packet.html": "packet"
  });

  const state = {
    viewKey: "reviewer",
    config: VIEW_DEFINITIONS.reviewer,
    payload: null,
    source: "none"
  };

  function hasOwn(object, key) {
    return Object.prototype.hasOwnProperty.call(object, key);
  }

  function selectView(search, pathname) {
    const requested = new URLSearchParams(search).get("view");
    if (requested && hasOwn(VIEW_DEFINITIONS, requested)) {
      return requested;
    }
    if (pathname && hasOwn(LEGACY_PATHS, pathname)) {
      return LEGACY_PATHS[pathname];
    }
    return "reviewer";
  }

  function humanize(value) {
    return String(value)
      .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
      .replace(/[_-]+/g, " ")
      .replace(/\b\w/g, (character) => character.toUpperCase());
  }

  function scalarText(value) {
    if (value === null) {
      return "Not provided";
    }
    if (typeof value === "boolean") {
      return value ? "Yes" : "No";
    }
    return String(value);
  }

  function createElement(tag, className, text) {
    const element = document.createElement(tag);
    if (className) {
      element.className = className;
    }
    if (text !== undefined) {
      element.textContent = text;
    }
    return element;
  }

  function clear(element) {
    while (element.firstChild) {
      element.removeChild(element.firstChild);
    }
  }

  function isScalar(value) {
    return value === null || ["string", "number", "boolean"].includes(typeof value);
  }

  function appendDefinitionList(container, object) {
    const list = createElement("dl");
    Object.entries(object).forEach(([key, value]) => {
      const term = createElement("dt", null, humanize(key));
      const description = createElement("dd");
      appendValue(description, value);
      list.append(term, description);
    });
    container.appendChild(list);
  }

  function appendValue(container, value) {
    if (isScalar(value)) {
      container.textContent = scalarText(value);
      return;
    }

    if (Array.isArray(value)) {
      if (value.length === 0) {
        container.textContent = "None";
        return;
      }
      const list = createElement("ul");
      value.forEach((item) => {
        const entry = createElement("li");
        if (isScalar(item)) {
          entry.textContent = scalarText(item);
        } else {
          const card = createElement("div", "detail-card");
          appendDefinitionList(card, item);
          entry.appendChild(card);
        }
        list.appendChild(entry);
      });
      container.appendChild(list);
      return;
    }

    appendDefinitionList(container, value);
  }

  function renderSummary(payload) {
    const grid = document.getElementById("summary-grid");
    clear(grid);
    const entries = Object.entries(payload).filter(([, value]) => isScalar(value));

    if (entries.length === 0) {
      grid.appendChild(createElement("p", "empty-state", "No scalar summary fields."));
      return;
    }

    entries.forEach(([key, value]) => {
      const card = createElement("dl", "summary-card");
      card.append(
        createElement("dt", null, humanize(key)),
        createElement("dd", null, scalarText(value))
      );
      grid.appendChild(card);
    });
  }

  function renderObjectSection(key, value) {
    const section = createElement("section", "detail-section");
    section.appendChild(createElement("h3", null, humanize(key)));
    const card = createElement("div", "detail-card");
    appendDefinitionList(card, value);
    section.appendChild(card);
    return section;
  }

  function renderArraySection(key, values) {
    const section = createElement("section", "detail-section");
    section.appendChild(createElement("h3", null, humanize(key)));
    if (values.length === 0) {
      section.appendChild(createElement("p", "empty-state", "No entries."));
      return section;
    }

    if (values.every(isScalar)) {
      const listCard = createElement("div", "list-card");
      appendValue(listCard, values);
      section.appendChild(listCard);
      return section;
    }

    const grid = createElement("div", "card-grid");
    values.forEach((value, index) => {
      const card = createElement("article", "detail-card");
      card.appendChild(createElement("h3", null, `${humanize(key)} ${index + 1}`));
      if (isScalar(value)) {
        card.appendChild(createElement("p", null, scalarText(value)));
      } else {
        appendDefinitionList(card, value);
      }
      grid.appendChild(card);
    });
    section.appendChild(grid);
    return section;
  }

  function renderDetails(payload) {
    const details = document.getElementById("detail-sections");
    clear(details);
    const entries = Object.entries(payload).filter(([, value]) => !isScalar(value));

    if (entries.length === 0) {
      details.appendChild(createElement("p", "empty-state", "No structured detail fields."));
      return;
    }

    entries.forEach(([key, value]) => {
      details.appendChild(
        Array.isArray(value)
          ? renderArraySection(key, value)
          : renderObjectSection(key, value)
      );
    });
  }

  function renderPayload(payload, source) {
    state.payload = payload;
    state.source = source;
    renderSummary(payload);
    renderDetails(payload);
    document.getElementById("raw-output").textContent = JSON.stringify(payload, null, 2);
  }

  function setLoadState(label, message, className) {
    document.getElementById("load-status").textContent = message;
    const chip = document.getElementById("load-state");
    chip.textContent = label;
    chip.className = `state-chip ${className}`;
  }

  function setActionStatus(message) {
    document.getElementById("action-status").textContent = message;
  }

  function renderNavigation() {
    const navigation = document.getElementById("viewer-navigation");
    clear(navigation);
    Object.entries(VIEW_DEFINITIONS).forEach(([key, config]) => {
      const link = createElement("a", null, config.label);
      link.href = `/evidence-viewer.html?view=${encodeURIComponent(key)}`;
      if (key === state.viewKey) {
        link.setAttribute("aria-current", "page");
      }
      navigation.appendChild(link);
    });
  }

  function renderHeader() {
    document.title = `${state.config.title} | LoadBalancerPro`;
    document.getElementById("view-eyebrow").textContent = state.config.eyebrow;
    document.getElementById("view-title").textContent = state.config.title;
    document.getElementById("view-description").textContent = state.config.description;
    document.getElementById("view-boundary").textContent = state.config.boundary;
    document.getElementById("view-endpoint").textContent = state.config.endpoint;
  }

  async function load() {
    setLoadState("LOADING", `Loading ${state.config.endpoint}`, "state-pending");
    setActionStatus("");
    try {
      const response = await fetch(state.config.endpoint, {
        method: "GET",
        headers: { Accept: "application/json" },
        credentials: "same-origin",
        cache: "no-store"
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const payload = await response.json();
      renderPayload(payload, "same-origin API");
      setLoadState(
        "LOADED",
        `Loaded ${state.config.endpoint} from the running application.`,
        "state-success"
      );
    } catch (error) {
      renderPayload(state.config.fallback, "static fallback");
      setLoadState(
        "FALLBACK",
        `The local API is unavailable (${error.message}). Showing bounded static guidance.`,
        "state-warning"
      );
    }
  }

  function markdownValue(value, depth) {
    if (isScalar(value)) {
      return scalarText(value);
    }
    if (Array.isArray(value)) {
      return value
        .map((item) => {
          if (isScalar(item)) {
            return `${"  ".repeat(depth)}- ${scalarText(item)}`;
          }
          const lines = Object.entries(item).map(
            ([key, nested]) =>
              `${"  ".repeat(depth + 1)}- ${humanize(key)}: ${markdownValue(nested, depth + 1)}`
          );
          return `${"  ".repeat(depth)}-\n${lines.join("\n")}`;
        })
        .join("\n");
    }
    return Object.entries(value)
      .map(
        ([key, nested]) =>
          `${"  ".repeat(depth)}- ${humanize(key)}: ${markdownValue(nested, depth + 1)}`
      )
      .join("\n");
  }

  function buildMarkdown() {
    const lines = [
      `# ${state.config.title}`,
      "",
      `Source: ${state.source}`,
      `Endpoint: ${state.config.endpoint}`,
      "",
      `Boundary: ${state.config.boundary}`,
      ""
    ];
    Object.entries(state.payload || {}).forEach(([key, value]) => {
      lines.push(`## ${humanize(key)}`, "", markdownValue(value, 0), "");
    });
    lines.push(
      "Browser-local handoff only. Review generated content for secrets before sharing."
    );
    return lines.join("\n");
  }

  function buildJson() {
    return JSON.stringify(
      {
        viewer: state.viewKey,
        source: state.source,
        endpoint: state.config.endpoint,
        boundary: state.config.boundary,
        payload: state.payload
      },
      null,
      2
    );
  }

  async function copyText(value, label) {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(value);
    } else {
      const input = createElement("textarea", "copy-buffer");
      input.value = value;
      input.setAttribute("readonly", "");
      document.body.appendChild(input);
      input.select();
      const copied = document.execCommand("copy");
      input.remove();
      if (!copied) {
        throw new Error("copy command was rejected");
      }
    }
    setActionStatus(`${label} copied in this browser.`);
  }

  function downloadText(filename, value, contentType) {
    const blob = new Blob([value], { type: contentType });
    const url = URL.createObjectURL(blob);
    const link = createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
    setActionStatus(`${filename} generated in this browser.`);
  }

  function handleAction(action) {
    if (!state.payload && action !== "refresh") {
      setActionStatus("Load a view before exporting it.");
      return;
    }
    try {
      if (action === "refresh") {
        void load();
      } else if (action === "copy-json") {
        void copyText(buildJson(), "JSON").catch((error) =>
          setActionStatus(`Copy failed: ${error.message}`)
        );
      } else if (action === "download-json") {
        downloadText(`${state.config.filename}.json`, buildJson(), "application/json");
      } else if (action === "copy-markdown") {
        void copyText(buildMarkdown(), "Markdown").catch((error) =>
          setActionStatus(`Copy failed: ${error.message}`)
        );
      } else if (action === "download-markdown") {
        downloadText(`${state.config.filename}.md`, buildMarkdown(), "text/markdown");
      } else if (action === "print") {
        window.print();
      }
    } catch (error) {
      setActionStatus(`Action failed: ${error.message}`);
    }
  }

  function bindActions() {
    const bindings = {
      "refresh-view": "refresh",
      "copy-json": "copy-json",
      "download-json": "download-json",
      "copy-markdown": "copy-markdown",
      "download-markdown": "download-markdown",
      "print-view": "print"
    };
    Object.entries(bindings).forEach(([id, action]) => {
      document.getElementById(id).addEventListener("click", () => handleAction(action));
    });
  }

  function mount() {
    state.viewKey = selectView(window.location.search, window.location.pathname);
    state.config = VIEW_DEFINITIONS[state.viewKey];
    renderNavigation();
    renderHeader();
    bindActions();
    void load();
  }

  window.LbpEvidenceViewer = Object.freeze({
    viewKeys: Object.freeze(Object.keys(VIEW_DEFINITIONS)),
    selectView,
    mount
  });

  document.addEventListener("DOMContentLoaded", mount, { once: true });
})();
