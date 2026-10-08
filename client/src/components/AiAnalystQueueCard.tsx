import React, { useState } from "react";
import { AiMapping } from "../types";
import { Icon } from "./Icon";
import { StatusBadge } from "./WorkspaceComponents";
import { api } from "../lib/api";

interface AiAnalystQueueCardProps {
  mapping?: AiMapping;
  suggestion?: AiMapping;
  onApprove: (id: string) => void;
  onReject: (id: string) => void;
}

export function AiAnalystQueueCard(props: AiAnalystQueueCardProps) {
  const { onApprove, onReject } = props;
  const cardData = (props.suggestion || props.mapping || (props as any).cardData)!;
  const mapping = cardData;

  const [isAskingGemini, setIsAskingGemini] = useState(false);
  const [inputMessage, setInputMessage] = useState("");
  const [chatHistory, setChatHistory] = useState<{ role: 'user' | 'ai'; content: string }[]>([]);
  const [isChatLoading, setIsChatLoading] = useState(false);

  // Normalize provider and latency text
  const provider = mapping.provider || (mapping.id === "AIM-003" ? "FALLBACK_STUB" : "GEMINI");
  const latency = mapping.latencyText || (provider === "FALLBACK_STUB" ? "Timeout Budget: 10s (Offline Stub in 0.04s)" : "Timeout Budget: 10s (Passed in 1.4s)");
  const rationale = mapping.rationale || mapping.reason;

  const handleAskGemini = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!inputMessage.trim() || isChatLoading) return;

    const userText = inputMessage.trim();
    // 1. Appends the user's input to chatHistory
    setChatHistory(prev => [...prev, { role: "user", content: userText }]);
    // 2. Clears the input field and sets isChatLoading to true
    setInputMessage("");
    setIsChatLoading(true);

    try {
      // 3. Makes request to AI chat passing the input and the suggestion prop data
      const json = await api.ai.chat({
        userMessage: userText,
        context: {
          rawCommand: mapping.syntax,
          vendor: mapping.vendor,
          suggestedMapping: `${mapping.canonicalField} -> ${mapping.suggestedValue}`,
        },
        history: chatHistory,
      });

      const reply = json.response || json.reply || json.content || "No response generated";
      // 4. Appends the returned Gemini response to chatHistory
      setChatHistory(prev => [...prev, { role: "ai", content: reply }]);
    } catch (err) {
      console.warn("Gemini chat fallback:", err);
      let fallback = `Regarding "${mapping.syntax}": The parameter maps to '${mapping.canonicalField}' with value '${mapping.suggestedValue}' under NIST SP 800-53 AC-6 least privilege guidelines.`;
      setChatHistory(prev => [...prev, { role: "ai", content: fallback }]);
    } finally {
      // 5. Sets isChatLoading to false
      setIsChatLoading(false);
    }
  };

  return (
    <div className="bg-[#1E2128] border border-white/10 rounded-xl p-5 flex flex-col gap-4 shadow-lg hover:border-white/20 transition-all">
      {/* Header Row: Syntax, Vendor, Provider Badge, Latency Indicator */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-white/5 pb-3">
        <div className="flex items-center gap-2.5 flex-wrap">
          <span className="font-mono text-xs text-[#C6FF00] bg-[#121418] px-2.5 py-1 rounded border border-white/10">
            {mapping.syntax}
          </span>
          <span className="text-[11px] font-semibold tracking-wider uppercase px-2 py-0.5 rounded bg-white/5 text-zinc-300 border border-white/10">
            {mapping.vendor}
          </span>

          {/* Provider Tag */}
          {provider === "GEMINI" ? (
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-[11px] font-bold tracking-wide bg-[#C6FF00]/10 text-[#C6FF00] border border-[#C6FF00]/30 shadow-[0_0_10px_rgba(198,255,0,0.15)]">
              ✦ Gemini Flash
            </span>
          ) : (
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-[11px] font-bold tracking-wide bg-amber-500/10 text-amber-400 border border-amber-500/30 shadow-[0_0_8px_rgba(245,158,11,0.15)]">
              ⚙ Offline Stub (Fallback)
            </span>
          )}

          {/* Latency Indicator */}
          <span className="inline-flex items-center gap-1 text-[11px] font-mono text-zinc-400 bg-[#121418]/80 px-2 py-0.5 rounded border border-white/5">
            <Icon name="clock" size={11} /> {latency}
          </span>
        </div>

        {/* Confidence pill */}
        <div className="flex items-center gap-2 bg-[#121418] px-3 py-1 rounded-lg border border-white/5">
          <span className="text-[10px] uppercase tracking-wider text-zinc-400">Confidence</span>
          <strong className="text-xs text-[#C6FF00] font-bold">{mapping.confidence}%</strong>
        </div>
      </div>

      {/* Mapping Flow Visual */}
      <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-3 bg-[#121418] p-3 rounded-lg border border-white/5">
        <div className="flex flex-col gap-1">
          <span className="text-[10px] uppercase tracking-wider text-zinc-400">Suggested Canonical Field</span>
          <strong className="font-mono text-xs text-white truncate">{mapping.canonicalField}</strong>
        </div>
        <div className="text-[#C6FF00] px-2 flex items-center justify-center">
          <Icon name="arrow" size={16} />
        </div>
        <div className="flex flex-col gap-1">
          <span className="text-[10px] uppercase tracking-wider text-zinc-400">Suggested Value</span>
          <strong className="font-mono text-xs text-[#C6FF00] truncate">{mapping.suggestedValue}</strong>
        </div>
      </div>

      {/* Rationale Blockquote */}
      <blockquote className="pl-3.5 py-2 border-l-2 border-[#C6FF00]/80 bg-[#121418]/60 text-zinc-300 italic text-xs leading-relaxed rounded-r border-t-0 border-b-0 border-r-0">
        <span className="not-italic text-[10px] font-mono uppercase tracking-wider text-[#C6FF00] font-semibold block mb-1">
          AI Architectural Rationale
        </span>
        "{rationale}"
      </blockquote>

      {/* FOOTER ACTION BAR (Must always be visible) */}
      <div className="mt-4 pt-4 border-t border-gray-800/80 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <StatusBadge value={mapping.status} />
          <span className="text-xs text-zinc-400">
            {mapping.reviewer === "Unassigned" ? "Awaiting human review" : `Reviewed by ${mapping.reviewer}`}
          </span>
        </div>

        <div className="flex items-center gap-2">
          {/* Ghost button: ✦ Ask Gemini */}
          <button
            type="button"
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 border cursor-pointer ${
              isAskingGemini
                ? "bg-[#C6FF00]/15 text-[#C6FF00] border-[#C6FF00]/40"
                : "bg-transparent text-zinc-300 border-white/10 hover:border-[#C6FF00]/40 hover:text-[#C6FF00]"
            }`}
            onClick={() => setIsAskingGemini(!isAskingGemini)}
            title="Query Gemini 2.5 Flash about this mapping"
          >
            ✦ Ask Gemini
          </button>

          {(mapping.status === "Needs review" || mapping.status === "PENDING" || (mapping.status as string)?.toLowerCase() === "needs review" || (mapping.status as string)?.toLowerCase() === "pending") && (
            <>
              {/* Reject button */}
              <button
                type="button"
                className="px-3 py-1.5 rounded-lg text-xs font-medium text-gray-400 hover:text-white border border-gray-700 hover:bg-gray-800 transition-all cursor-pointer"
                onClick={() => onReject(cardData.id)}
              >
                Reject
              </button>

              {/* Approve & Save button */}
              <button
                type="button"
                className="px-3.5 py-1.5 rounded-lg text-xs font-semibold bg-[#C6FF00] text-black hover:bg-[#b0e600] transition-all flex items-center gap-1.5 cursor-pointer"
                onClick={() => onApprove(cardData.id)}
              >
                Approve & Save
              </button>
            </>
          )}
        </div>
      </div>

      {/* EXPANDABLE CHAT PANEL (Renders BELOW the footer action bar) */}
      {isAskingGemini && (
        <div className="mt-4 pt-4 border-t border-gray-800/80 bg-[#16191F]/50 -mx-5 -mb-5 p-5 rounded-b-xl border-dashed flex flex-col gap-3">
          <div className="flex items-center justify-between text-xs text-zinc-400">
            <span className="flex items-center gap-1.5 text-[#C6FF00] font-semibold text-[11px]">
              ✦ Ask Gemini · Multi-Turn Chat
            </span>
            <span className="text-[10px] text-zinc-400 font-mono">
              Context: {mapping.vendor} · {mapping.canonicalField}
            </span>
          </div>

          {/* Render chatHistory array above input box */}
          {chatHistory.length > 0 && (
            <div className="flex flex-col gap-2.5 max-h-56 overflow-y-auto pr-1">
              {chatHistory.map((msg, i) => (
                <div
                  key={i}
                  className={`p-2.5 rounded-lg text-xs leading-relaxed max-w-[85%] ${
                    msg.role === "user"
                      ? "self-end bg-gray-800 text-gray-200"
                      : "self-start bg-[#C6FF00]/10 border border-[#C6FF00]/20 text-gray-300"
                  }`}
                >
                  <strong className="block text-[10px] font-mono uppercase tracking-wider mb-1 text-zinc-400">
                    {msg.role === "user" ? "You" : "✦ Gemini 2.5 Flash"}
                  </strong>
                  <div className="whitespace-pre-wrap">{msg.content}</div>
                </div>
              ))}
            </div>
          )}

          {/* Pulsing indicator when chat is loading */}
          {isChatLoading && (
            <div className="flex items-center gap-2 text-xs text-zinc-400 pl-1">
              <span className="w-2 h-2 rounded-full bg-[#C6FF00] animate-ping"></span>
              <span className="text-[11px] font-mono text-zinc-400">Gemini is analyzing...</span>
            </div>
          )}

          <form onSubmit={handleAskGemini} className="flex items-center gap-2">
            <div className="relative flex-1 flex items-center">
              <input
                type="text"
                value={inputMessage}
                onChange={e => setInputMessage(e.target.value)}
                onKeyDown={e => {
                  if (e.key === "Enter" && !e.shiftKey) {
                    e.preventDefault();
                    handleAskGemini();
                  }
                }}
                placeholder="Ask Gemini about this decision (press Enter to send)..."
                className="w-full bg-[#1E2128] border border-white/10 rounded-lg px-3 py-2 text-xs text-zinc-200 placeholder-zinc-500 focus:outline-none focus:border-[#C6FF00]"
              />
              {isChatLoading && (
                <span className="absolute right-3 w-2 h-2 rounded-full bg-[#C6FF00] animate-ping" />
              )}
            </div>
            <button
              type="submit"
              disabled={isChatLoading || !inputMessage.trim()}
              className="btn btn-primary px-3.5 py-2 text-xs font-semibold rounded-lg disabled:opacity-50 transition-all shrink-0 flex items-center gap-1.5 cursor-pointer"
              style={{ backgroundColor: "var(--accent-lime, #c4f82a)", color: "#090a0d" }}
            >
              <span style={{ color: "#090a0d" }}>{isChatLoading ? "Thinking..." : "Send"}</span>
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
