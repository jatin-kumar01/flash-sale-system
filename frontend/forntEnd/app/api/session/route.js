import { NextResponse } from "next/server";

const frontendStartedAt = Date.now();

export async function GET() {
    return NextResponse.json({
        startedAt: frontendStartedAt,
    });
}
