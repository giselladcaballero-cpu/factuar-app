// Mercado Pago's back_url for the subscription checkout started in
// create-subscription. Mercado Pago redirects here with the preapproval id
// once the merchant finishes (or abandons) authorizing the recurring
// charge. Verifies the real status server-side (never trust the query
// params alone — they're not signed) against Vektor Go's own Mercado Pago
// account, then bounces to the app via factuar://subscription-activated.

const MP_PREAPPROVAL_URL = "https://api.mercadopago.com/preapproval";
const APP_SCHEME = "factuar://subscription-activated";

function textResponse(body: string, status = 200): Response {
  const bytes = new TextEncoder().encode(body);
  return new Response(bytes, {
    status,
    headers: { "Content-Type": "text/plain; charset=utf-8", "Content-Length": String(bytes.byteLength) },
  });
}

Deno.serve(async (req: Request) => {
  const url = new URL(req.url);
  const preapprovalId = url.searchParams.get("preapproval_id");

  if (!preapprovalId) {
    return textResponse("Mercado Pago no envió un ID de suscripción válido. Volvé a la app e intentá de nuevo.", 400);
  }

  const businessToken = Deno.env.get("VEKTOR_BUSINESS_MP_ACCESS_TOKEN");
  if (!businessToken) {
    return textResponse("Falta configurar VEKTOR_BUSINESS_MP_ACCESS_TOKEN en los secrets de Supabase.", 500);
  }

  try {
    // Ask Mercado Pago directly instead of trusting the redirect's own
    // query params — those aren't signed and could be tampered with.
    const statusResponse = await fetch(`${MP_PREAPPROVAL_URL}/${preapprovalId}`, {
      headers: { Authorization: `Bearer ${businessToken}` },
    });
    const data = await statusResponse.json();

    if (!statusResponse.ok) {
      return textResponse(`No se pudo verificar la suscripción: ${data.message || "error desconocido"}.`);
    }

    // "authorized" is the only status that means the recurring charge is
    // actually active; anything else (pending, cancelled, paused) must not
    // unlock the app.
    const status = data.status as string;
    const deepLink = `${APP_SCHEME}?preapproval_id=${encodeURIComponent(preapprovalId)}&status=${encodeURIComponent(status)}`;

    return new Response(null, { status: 302, headers: { Location: deepLink } });
  } catch (e) {
    return textResponse(`Error inesperado verificando la suscripción: ${(e as Error).message}. Volvé a intentarlo desde la app.`);
  }
});
