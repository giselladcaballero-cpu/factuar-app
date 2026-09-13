// Handles the OAuth redirect from Mercado Pago after a user approves linking
// their account to the Vektor Go app. Exchanges the authorization code for an
// access token (using the Client Secret, kept server-side only), fetches the
// account's display name, then bounces the browser to a custom URI scheme so
// the Android app picks it up directly — no polling, no database.

const MP_TOKEN_URL = "https://api.mercadopago.com/oauth/token";
const MP_ME_URL = "https://api.mercadopago.com/users/me";
const APP_SCHEME = "factuar://mp-connected";

function htmlPage({ title, message, deepLink }: { title: string; message: string; deepLink?: string }): string {
  const redirectScript = deepLink
    ? `<script>window.location.replace(${JSON.stringify(deepLink)});</script>`
    : "";
  const linkButton = deepLink
    ? `<a href="${deepLink}" style="display:inline-block;margin-top:16px;padding:12px 20px;background:#009ee3;color:#fff;border-radius:8px;text-decoration:none;font-weight:bold;">Volver a la app</a>`
    : "";
  return `<!doctype html>
<html lang="es"><head><meta charset="utf-8"><title>${title}</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body style="font-family:sans-serif;text-align:center;padding:40px 20px;background:#111;color:#eee;">
  <h2>${title}</h2>
  <p>${message}</p>
  ${linkButton}
  ${redirectScript}
</body></html>`;
}

function htmlResponse(body: string, status = 200): Response {
  return new Response(body, {
    status,
    headers: { "Content-Type": "text/html; charset=utf-8" },
  });
}

Deno.serve(async (req: Request) => {
  const url = new URL(req.url);
  const code = url.searchParams.get("code");
  const state = url.searchParams.get("state");
  const mpError = url.searchParams.get("error");

  if (mpError) {
    return htmlResponse(
      htmlPage({
        title: "Autorización cancelada",
        message: "No se completó la vinculación con Mercado Pago. Podés volver a intentarlo desde la app.",
      }),
    );
  }

  if (!code) {
    return htmlResponse(
      htmlPage({
        title: "Falta el código de autorización",
        message: "Mercado Pago no envió un código válido. Volvé a la app e intentá de nuevo.",
      }),
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
        htmlPage({
          title: "No se pudo vincular la cuenta",
          message: `Mercado Pago rechazó la autorización: ${tokenData.message || tokenData.error || "error desconocido"}.`,
        }),
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

    return htmlResponse(
      htmlPage({
        title: "¡Cuenta de Mercado Pago vinculada!",
        message: "Ya podés volver a la app Vektor Go.",
        deepLink,
      }),
    );
  } catch (e) {
    return htmlResponse(
      htmlPage({
        title: "Error inesperado",
        message: `Ocurrió un problema vinculando la cuenta: ${(e as Error).message}. Volvé a intentarlo desde la app.`,
      }),
    );
  }
});
