// Handles the OAuth redirect from Mercado Pago after a user approves linking
// their account to the Vektor Go app. Exchanges the authorization code for an
// access token (using the Client Secret, kept server-side only), fetches the
// account's display name, then bounces the browser to a custom URI scheme so
// the Android app picks it up directly — no polling, no database.
//
// On success this issues a plain HTTP redirect (Location header) straight to
// factuar://mp-connected, instead of an HTML page whose <script> does
// window.location.replace(). Supabase's edge gateway forces a
// Content-Security-Policy: sandbox + Content-Type: text/plain +
// X-Content-Type-Options: nosniff on non-JSON function responses (undocumented,
// found by testing — real mobile browsers send Accept-Encoding, which triggers
// it; plain `curl` without that header doesn't, which is why it wasn't obvious
// at first). That sandboxing blocks the redirect script from ever running and
// renders the HTML as literal text instead. A raw 302 doesn't depend on the
// browser executing anything, so it isn't affected.
//
// Error cases still return a small HTML page — nothing there needs to
// execute script, so the CSP sandbox doesn't matter for them; it's fine if a
// browser renders it as plain text too, since it's just a message for the
// person to read.

const MP_TOKEN_URL = "https://api.mercadopago.com/oauth/token";
const MP_ME_URL = "https://api.mercadopago.com/users/me";
const APP_SCHEME = "factuar://mp-connected";

function errorPage(title: string, message: string): string {
  return `<!doctype html>
<html lang="es"><head><meta charset="utf-8"><title>${title}</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body style="font-family:sans-serif;text-align:center;padding:40px 20px;background:#111;color:#eee;">
  <h2>${title}</h2>
  <p>${message}</p>
</body></html>`;
}

function htmlResponse(body: string, status = 200): Response {
  const bytes = new TextEncoder().encode(body);
  return new Response(bytes, {
    status,
    headers: {
      "Content-Type": "text/html; charset=utf-8",
      "Content-Length": String(bytes.byteLength),
    },
  });
}

Deno.serve(async (req: Request) => {
  const url = new URL(req.url);
  const code = url.searchParams.get("code");
  const state = url.searchParams.get("state");
  const mpError = url.searchParams.get("error");

  if (mpError) {
    return htmlResponse(
      errorPage(
        "Autorización cancelada",
        "No se completó la vinculación con Mercado Pago. Podés volver a intentarlo desde la app.",
      ),
    );
  }

  if (!code) {
    return htmlResponse(
      errorPage(
        "Falta el código de autorización",
        "Mercado Pago no envió un código válido. Volvé a la app e intentá de nuevo.",
      ),
      400,
    );
  }

  try {
    const tokenResponse = await fetch(MP_TOKEN_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        client_id: Deno.env.get("MP_CLIENT_ID"),
        client_secret: Deno.env.get("MP_CLIENT_SECRET"),
        grant_type: "authorization_code",
        code,
        redirect_uri: Deno.env.get("MP_REDIRECT_URI"),
        // PKCE: `state` is the code_verifier generated in mp-authorize,
        // echoed back unchanged by Mercado Pago.
        code_verifier: state,
      }),
    });

    const tokenData = await tokenResponse.json();
    if (!tokenResponse.ok || !tokenData.access_token) {
      return htmlResponse(
        errorPage(
          "No se pudo vincular la cuenta",
          `Mercado Pago rechazó la autorización: ${tokenData.message || tokenData.error || "error desconocido"}.`,
        ),
      );
    }

    const meResponse = await fetch(MP_ME_URL, {
      headers: { Authorization: `Bearer ${tokenData.access_token}` },
    });
    const me = meResponse.ok ? await meResponse.json() : {};

    const name = [me.first_name, me.last_name].filter(Boolean).join(" ") || me.nickname || "";

    const params = new URLSearchParams({
      access_token: tokenData.access_token,
      user_id: String(tokenData.user_id || me.id || ""),
      name,
      state: state || "",
    });
    const deepLink = `${APP_SCHEME}?${params.toString()}`;

    return new Response(null, {
      status: 302,
      headers: { Location: deepLink },
    });
  } catch (e) {
    return htmlResponse(
      errorPage(
        "Error inesperado",
        `Ocurrió un problema vinculando la cuenta: ${(e as Error).message}. Volvé a intentarlo desde la app.`,
      ),
    );
  }
});
